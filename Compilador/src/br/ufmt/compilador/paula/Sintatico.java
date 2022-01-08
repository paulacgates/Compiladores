package br.ufmt.compilador.paula;

import java.util.*;

public class Sintatico {

	private LexScanner lexico;
	private Token simbolo;
	private List<Token> simbolos;
	private int indiceTokenAtual = 0;
	private int tipo;
	private int temp;
	private int topoPilhaDados = -1;
	private int topoPilhaComandos = -1;
	private Stack<String> pilhaComandos = new Stack<String>();
	private Map<String, Simbolo> tabelaSimbolo = new HashMap<>();
	private StringBuilder codigo = new StringBuilder();
	private int escopoDasVariaveis = 0;
	private boolean isProcedure = false;
	private boolean isParametro = false;
	private int quantidadeVariaveisEParametrosParaDesalocarProcedure = 0;

	public Sintatico(String arq) {
		lexico = new LexScanner(arq);
		simbolos = lexico.tokens();
	}

	public void analise() {
		obtemSimbolo();
		program();
		if (simbolo == null) {
			System.out.println("Tudo certo!");
			System.out.println(String.join("\n", pilhaComandos));
		} else {
			throw new RuntimeException("Erro sintático esperado fim de cadeia encontrado: " + simbolo.getTermo());
		}
	}

	private String geraTemp() {
		return "t" + temp++;
	}

	private void code(String op, String arg1, String arg2, String result) {
		codigo.append(op + ";" + arg1 + ";" + arg2 + ";" + result + "\n");
	}

	private void code(String op) {
		topoPilhaComandos++;
		pilhaComandos.push(op);
	}

	private void code(String op, int linha) {
		topoPilhaComandos++;
		pilhaComandos.push(op + " " + linha);
	}

	private void code(String op, String placeholder) {
		topoPilhaComandos++;
		pilhaComandos.push(op + " " + placeholder);
	}

	private void obtemSimbolo() {
		if (indiceTokenAtual < simbolos.size()) {
			simbolo = simbolos.get(indiceTokenAtual++);
			if (simbolo.getTipo() == Token.IDENT){
				simbolo.setTermo(nomeParaVariavel());
			}
		} else {
			simbolo = null;
		}
	}

	private void retrocedeSimbolo() {
		if (indiceTokenAtual > 0) {
			indiceTokenAtual--;
		}
	}

	private boolean isTermo(String termo) {
		return (simbolo != null && simbolo.getTermo().equals(termo));
	}

	private boolean isTipo(int tipo) {
		return (simbolo != null && simbolo.getTipo() == tipo);
	}

	private void program() {
		if (isTermo("program")) {
			obtemSimbolo();
			if (isTipo(Token.IDENT)) {
				code("INPP");
				corpo();
				if (isTermo(".")) {
					code("PARA", "", "", "");
					obtemSimbolo();
				} else {
					throw new RuntimeException("Erro Sintático! Esperado '.' Encontrado: " + simbolo.getTermo());
				}
			} else {
				throw new RuntimeException("Erro Sintático! Esperado 'Identificador' Encontrado: " + simbolo.getTipo());
			}
		} else {
			throw new RuntimeException("Erro Sintático! Esperado 'program' Encontrado: " + simbolo.getTermo());
		}
	}

	private void corpo() {
		dc();
		if (isTermo("begin")) {
			obtemSimbolo();
			comandos();
			if (isTermo("end")) {
				code("PARA");
				obtemSimbolo();
			} else {
				throw new RuntimeException("Erro Sintático! Esperado 'end' Encontrado: " + simbolo.getTermo());
			}
		} else {
			throw new RuntimeException("Erro Sintático! Esperado 'begin' Encontrado: " + simbolo.getTermo());
		}
	}

	private void dc() {
		obtemSimbolo();
		if (isTermo("real") || isTermo("integer")) {
			dc_v();
			if (isTermo(";")) {
				mais_dc();
			}
		} else if (isTermo("procedure")) {
			code("DSVI", ":procedure");
			dc_p();
			replacePlaceholder(":procedure", Integer.toString(topoPilhaComandos));
		}
	}

	private void replacePlaceholder(String placeholder, String conteudo) {
		for (int i = pilhaComandos.size() - 1; i >= 0; i--) {
			String linhaDeComando = pilhaComandos.get(i);
			if (linhaDeComando.contains(placeholder)) {
				pilhaComandos.set(i, linhaDeComando.replace(placeholder, conteudo));
				return;
			}
		}
	}

	private void dc_p() {
		isProcedure = true;

		obtemSimbolo();
		addProcedimentoTabelaDeSimbolos();
		escopoDasVariaveis++;

		parametros();
		corpo_p();
		escopoDasVariaveis--;

		isProcedure = false;
	}

	private void parametros() {
		isParametro = true;
		obtemSimbolo();
		if (isTermo("(")) {
			lista_par();
			if (!isTermo(")")) {
				throw new RuntimeException("Erro Sintático! Esperado ')' Encontrado: " + simbolo.getTermo());
			}
		}
		isParametro = false;
	}

	private void lista_par() {
		obtemSimbolo();
		tipo_var();

		obtemSimbolo();
		if (!isTermo(":")) {
			throw new RuntimeException("Erro Sintático! Esperado ':' Encontrado: " + simbolo.getTermo());
		}
		variaveis();
		mais_par();
	}

	private void mais_par() {
		if (isTermo(";")) {
			lista_par();
		}
	}

	private void corpo_p() {
		obtemSimbolo();
		dc_loc();

		if (!isTermo("begin")) {
			throw new RuntimeException("Erro Sintático! Esperado 'begin' Encontrado: " + simbolo.getTermo());
		}

		obtemSimbolo();
		comandos();

		if (!isTermo("end")) {
			throw new RuntimeException("Erro Sintático! Esperado 'end' Encontrado: " + simbolo.getTermo());
		}

		code("DESM", quantidadeVariaveisEParametrosParaDesalocarProcedure);
		quantidadeVariaveisEParametrosParaDesalocarProcedure = 0;
		code("RTPR");

		obtemSimbolo();
	}

	private void dc_loc() {
		if (isTermo("real") || isTermo("integer")) {
			dc_v();
			mais_dcloc();
		}
	}

	private void mais_dcloc() {
		if (isTermo(";")) {
			obtemSimbolo();
			dc_loc();
		}
	}

	private void lista_arg() {
		if(isTermo("(")) {
			argumentos();

			if (!isTermo(")")) {
				throw new RuntimeException("Erro Sintático! Esperado ')' Encontrado" + simbolo.getTermo());
			}
			obtemSimbolo();
		}
	}

	private void argumentos() {
		obtemSimbolo();
		code("PARAM", tabelaSimbolo.get(simbolo.getTermo()).getEndRel());

		mais_ident();
	}

	private void mais_ident() {
		obtemSimbolo();
		if (isTermo(",")) {
			argumentos();
		}
	}

	private void mais_dc() {
		if (isTermo(";")) {
			dc();
		} else {
			throw new RuntimeException("Erro Sintático! Esperado ';' Encontrado: " + simbolo.getTermo());
		}
	}

	private void dc_v() {
		tipo_var();
		obtemSimbolo();
		if (isTermo(":")) {
			variaveis();
		} else {
			throw new RuntimeException("Erro Sintático! Esperado ':' Encontrado: " + simbolo.getTermo());
		}
	}

	private void tipo_var() {
		if (isTermo("real")){
			this.tipo = Token.REAL;
		} else if (isTermo("integer")){
			this.tipo = Token.NUMERO;
		} else {
			throw new RuntimeException("Erro Sintático! Esperado real ou integer Encontrado " + simbolo.getTermo());
		}
	}

	private void variaveis() {
		obtemSimbolo();
		if (isTipo(Token.IDENT)) {
			if (isProcedure) {
				quantidadeVariaveisEParametrosParaDesalocarProcedure++;
			}

			if (isParametro) {
				addArgumentoTabelaDeSimbolos();
			} else {
				addVariavelTabelaDeSimbolos();
			}
			obtemSimbolo();
			if (isTermo(",")) {
				mais_var();
			}
		} else {
			throw new RuntimeException("Erro Sintático! identificador esperado Encontrado " + simbolo.getTermo());
		}
	}

	private String nomeParaVariavel() {
		return ".".repeat(escopoDasVariaveis).concat(simbolo.getTermo());
	}

	private void addVariavelTabelaDeSimbolos() {
		Simbolo simboloAux = new Simbolo(simbolo.getTipo(), simbolo.getTermo(), ++topoPilhaDados);
		addSimboloNaTabela(simboloAux);
		code("ALME", 1);
	}

	private void addProcedimentoTabelaDeSimbolos() {
		Simbolo simboloAux = new Simbolo(simbolo.getTipo(), simbolo.getTermo(), topoPilhaComandos + 1, -1);
		addSimboloNaTabela(simboloAux);
		topoPilhaDados++;
	}

	private void addArgumentoTabelaDeSimbolos() {
		Simbolo simboloAux = new Simbolo(simbolo.getTipo(), simbolo.getTermo(), ++topoPilhaDados);
		addSimboloNaTabela(simboloAux);
	}

	private void addSimboloNaTabela(Simbolo simboloAux) {
		if (tabelaSimbolo.containsKey(simbolo.getTermo())){
			throw new RuntimeException("Erro semântico! identificador já encontrado: " + simbolo.getTermo());
		} else {
			tabelaSimbolo.put(simbolo.getTermo(), simboloAux);
			code("ALME", "1", "", simbolo.getTermo());
		}
	}

	private void mais_var() {
		variaveis();
	}

	private void comandos() {
		comando();
		if (isTermo(";")) {
			mais_comandos();
		}
	}

	private void mais_comandos() {
		if (isTermo(";")) {
			obtemSimbolo();
			comandos();
		}
	}

	private void comando() {
		if (isTermo("read") || isTermo("write")) {
			String oper = simbolo.getTermo();
			obtemSimbolo();
			if (isTermo("(")) {
				obtemSimbolo();
				if (isTipo(Token.IDENT)) {
					Token simboloAux = simbolo;
					if (!tabelaSimbolo.containsKey(simbolo.getTermo())){
                        throw new RuntimeException("Erro semântico! identificador não foi declarado " + simbolo.getTermo());
                    }
					obtemSimbolo();
					if (isTermo(")")) {
						int endRel = tabelaSimbolo.get(simboloAux.getTermo()).getEndRel();
						if (oper.equals("read")) {
							code("LEIT");
							code("ARMZ", endRel);
						} else if (oper.equals("write")){
							code("CRVL", endRel);
							code("IMPR");
						}
						obtemSimbolo();
					} else {
						throw new RuntimeException("Erro Sintático! Esperado ')' Encontrado" + simbolo.getTermo());
					}
				} else {
					throw new RuntimeException(
							"Erro Sintático! Esperado um Identificador Encontrado" + simbolo.getTipo());
				}
			} else {
				throw new RuntimeException("Erro Sintático! esperado '(' Encontrado " + simbolo.getTermo());
			}
		} else if (isTipo(Token.IDENT)) {
			if (!tabelaSimbolo.containsKey(simbolo.getTermo())){
                throw new RuntimeException("Erro semântico! identificador não foi declarado " + simbolo.getTermo());
            }

			Token simboloAux = simbolo;
			restoIdent();

			// Variável ou parâmetro
			if (tabelaSimbolo.get(simboloAux.getTermo()).getEndRel() >= 0) {
				code("ARMZ", tabelaSimbolo.get(simboloAux.getTermo()).getEndRel());
			}

		} else if (isTermo("if")) {
			obtemSimbolo();
			String condicaoD = condicao();
			if (isTermo("then")) {
				code("DSVF", ":dsvf");
				obtemSimbolo();
				comandos();
				code("DSVI", ":dsvi");

				int finalIf = topoPilhaComandos + 1;
				pfalsa();
				int finalElse = topoPilhaComandos + 1;

				replacePlaceholder(":dsvf", Integer.toString(finalIf));
				replacePlaceholder(":dsvi", Integer.toString(finalElse));


				if (isTermo("$")) {
					obtemSimbolo();
				} else {
					throw new RuntimeException("Erro Sintático! Esperado '$' Encontrado" + simbolo.getTermo());
				}
			} else {
				throw new RuntimeException("Erro Sintático! Esperado 'then' Encontrado" + simbolo.getTermo());
			}
		} else if (isTermo("while")) {
			obtemSimbolo();
			int dsviAux = topoPilhaComandos + 1;
			String condicaoD = condicao();
			if (isTermo("do")) {
				code("DSVF", ":dsvf");
				obtemSimbolo();
				comandos();
				code("DSVI", dsviAux);
				replacePlaceholder(":dsvf", Integer.toString(topoPilhaComandos + 1));

				if (isTermo("$")) {
					obtemSimbolo();
				} else {
					throw new RuntimeException("Erro Sintático! Esperado '$' Encontrado" + simbolo.getTermo());
				}
			} else {
				throw new RuntimeException("Erro Sintático! Esperado 'then' Encontrado" + simbolo.getTermo());
			}
		} else {
			throw new RuntimeException("Erro Sintático! Esperado 'if' Encontrado " + simbolo.getTermo());
		}
	}

	private void restoIdent() {
		Token simboloAux = simbolo;
		obtemSimbolo();
		if (isTermo(":=")) {
			obtemSimbolo();
			expressao();
		} else if (isTermo("(")){
			code("PUSHER", ":pusher");
			lista_arg();
			code("CHPR", tabelaSimbolo.get(simboloAux.getTermo()).getPrimInstr());
			replacePlaceholder(":pusher", Integer.toString(topoPilhaComandos + 1));
		}
	}

	private String condicao() {
		String expressaoD = expressao();
		String relacaoD = relacao();
		String expressaoLD = expressao();

		switch (relacaoD) {
			case "=":
				code("CPIG");
				break;
			case "<>":
				code("CDES");
				break;
			case ">=":
				code("CMAI");
				break;
			case "<=":
				code("CPMI");
				break;
			case ">":
				code("CPMA");
				break;
			case "<":
				code("CPME");
				break;
		}

		String condicaoD = geraTemp();
		code(relacaoD, expressaoD, expressaoLD, condicaoD);
		return condicaoD;

	}

	private String relacao() {
		if (isTermo("=") || !isTermo("<>") || !isTermo(">=") || !isTermo("<=") || !isTermo(">") || !isTermo("<")) {
			String relacaoD = simbolo.getTermo();
			obtemSimbolo();
			return relacaoD;
		} else {
			throw new RuntimeException("Erro Sintático! Esperado = | <> | >= | <= | > | <  Encontrado" + simbolo.getTermo());
		}

	}

	private String expressao() {
		String termoD = termo();
		String outros_termosD = outros_termos(termoD);
		return outros_termosD;
	}

	private String termo() {
		String op_unD = op_un();
		String fatorD = fator();
		String mais_fatoresD = mais_fatores(fatorD);
		return mais_fatoresD;

	}

	private String op_un() {
		if (isTermo("-")) {
			code("INVE");
			String op_unD = simbolo.getTermo();
			obtemSimbolo();
			return op_unD;
		}
		return "";

	}

	private String fator() {
		if (isTipo(Token.IDENT) || isTipo(Token.NUMERO) || isTipo(Token.REAL)) {
			if (isTipo(Token.IDENT)) {
				code("CRVL", tabelaSimbolo.get(simbolo.getTermo()).getEndRel());
			} else {
				code("CRCT", simbolo.getTermo());
			}

			String fatorD = simbolo.getTermo();
			obtemSimbolo();
			return fatorD;
		} else if (isTermo("(")) {
			obtemSimbolo();
			String fatorD = expressao();
			if (isTermo(")")) {
				obtemSimbolo();
				return fatorD;
			} else {
				throw new RuntimeException("Erro Sintático! Esperado '$' Encntrado" + simbolo.getTermo());
			}
		} else {
			throw new RuntimeException("Erro Sintático! Esperado token do tipo Identificador, Inteiro ou Real Encntrado" + simbolo.getTermo());
		}
	}

	private String outros_termos(String outrosE) {
		if (isTermo("+") || isTermo("-")) {
			String op_adD = op_ad();
			String termoD = termo();

			switch (op_adD) {
				case "+":
					code("SOMA");
					break;
				case "-":
					code("SUBT");
					break;
			}

			String outrosD = geraTemp();
			String outrosLD = outros_termos(termoD);
			code(op_adD, outrosE, outrosLD, outrosD);
			return outrosD;
		}
		return outrosE;
	}

	private String op_ad() {
		if (isTermo("+") || isTermo("-")) {
			String op_adD = simbolo.getTermo();
			obtemSimbolo();
			return op_adD;
		} else {
			throw new RuntimeException("Erro Sintático! Esperado + ou - Encntrado" + simbolo.getTermo());
		}
	}

	private String mais_fatores(String mais_fatoresE) {
		if (isTermo("*") || isTermo("/")) {
			String op_mulD = op_mul();
			String fatorD = fator();

			switch (op_mulD) {
				case "*":
					code("MULT");
					break;
				case "/":
					code("DIVI");
					break;
			}

			String mais_fatoresD = geraTemp();
			String mais_fatoresLD = mais_fatores(fatorD);
			code(op_mulD, mais_fatoresE, mais_fatoresLD, mais_fatoresD);
			return mais_fatoresD;
		}
		return mais_fatoresE;

	}

	private String op_mul() {
		if (isTermo("*") || isTermo("/")) {
			String op_mulD = simbolo.getTermo();
			obtemSimbolo();
			return op_mulD;
		} else {
			throw new RuntimeException("Erro Sintático! Esperado * ou / Encontrado" + simbolo.getTermo());
		}
	}

	private void pfalsa() {
		if (isTermo("else")) {
			code("goto", "&", "", "");
			obtemSimbolo();
			comandos();
		}
	}
}
