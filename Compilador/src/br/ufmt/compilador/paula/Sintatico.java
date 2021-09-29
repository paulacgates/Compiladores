package br.ufmt.compilador.paula;

import java.util.HashMap;
import java.util.Map;

public class Sintatico {

	private LexScanner lexico;
	private Token simbolo;
	private int tipo;
	private int temp;
	private Map<String, Simbolo> tabelaSimbolo = new HashMap<>();
	private StringBuilder codigo = new StringBuilder("operador;arg1;arg2;result\n");

	public Sintatico(String arq) {
		lexico = new LexScanner(arq);
	}

	public void analise() {
		obtemSimbolo();
		program();
		if (simbolo == null) {
			System.out.println("Tudo certo!");
			System.out.println(codigo.toString());
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

	private void obtemSimbolo() {
		simbolo = lexico.nextToken();
	}

	private boolean isTermo(String termo) {
		return (simbolo != null && simbolo.getTermo().equals(termo));
	}

	private boolean isTipo(int tipo) {
		return (simbolo != null && simbolo.getTipo() == tipo);
	}

	private void program() {
		System.out.println("programa");
		if (isTermo("program")) {
			obtemSimbolo();
			if (isTipo(0)) {
				obtemSimbolo();
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
		System.out.println("corpo");
		dc();
		if (isTermo("begin")) {
			obtemSimbolo();
			comandos();
			if (isTermo("end")) {
				obtemSimbolo();
			} else {
				throw new RuntimeException("Erro Sintático! Esperado 'end' Encontrado: " + simbolo.getTermo());
			}

		} else {
			throw new RuntimeException("Erro Sintático! Esperado 'begin' Encontrado: " + simbolo.getTermo());
		}
	}

	private void dc() {
		System.out.println("dc");
		if (isTermo("real") || isTermo("integer")) {
			dc_v();
			if (isTermo(";")) {
				mais_dc();
			}
			
		}
	}

	private void mais_dc() {
		System.out.println("mais_dc");
		if (isTermo(";")) {
			obtemSimbolo();
			dc();
		} else {
			throw new RuntimeException("Erro Sintático! Esperado ';' Encontrado: " + simbolo.getTermo());
		}
	}

	private void dc_v() {
		System.out.println("dc_v");
		tipo_var();
		if (isTermo(":")) {
			obtemSimbolo();
			variaveis();
		} else {
			throw new RuntimeException("Erro Sintático! Esperado ':' Encontrado: " + simbolo.getTermo());
		}
	}

	private void tipo_var() {
		System.out.println("tipo_var");
		if (isTermo("real")){
			this.tipo = Token.REAL;
			obtemSimbolo();
		} else if (isTermo("integer")){
			this.tipo = Token.NUMERO;
			obtemSimbolo();
		} else {
			throw new RuntimeException("Erro Sintático! Esperado real ou integer Encontrado " + simbolo.getTermo());
		}
	}

	private void variaveis() {
		System.out.println("variaveis");
		if (isTipo(0)) {
			if (tabelaSimbolo.containsKey(simbolo.getTermo())){
				throw new RuntimeException("Erro semântico! identificador já encontrado: " + simbolo.getTermo());
			} else {
				tabelaSimbolo.put(simbolo.getTermo(), new Simbolo(this.tipo, simbolo.getTermo()));
				code("ALME", this.tipo == Token.NUMERO ? "0" : "0.0", "", simbolo.getTermo());
			}
			obtemSimbolo();
			if (isTermo(",")) {
				mais_var();
			}
		} else {
			throw new RuntimeException("Erro Sintático! identificador esperado Encontrado " + simbolo.getTermo());
		}
	}

	private void mais_var() {
		obtemSimbolo();
		variaveis();

	}

	private void comandos() {
		System.out.println("comandos");
		comando();
		if (isTermo(";")) {
			mais_comandos();
		}
	}

	private void mais_comandos() {
		System.out.println("mais_comandos");
		if (isTermo(";")) {
			obtemSimbolo();
			comandos();
		}
	}

	private void comando() {
		System.out.println("comando");
		if (isTermo("read") || isTermo("write")) {
			String oper = simbolo.getTermo();
			obtemSimbolo();
			if (isTermo("(")) {
				obtemSimbolo();
				if (isTipo(0)) {
					if (!tabelaSimbolo.containsKey(simbolo.getTermo())){
                        throw new RuntimeException("Erro semântico! identificador não foi declarado " + simbolo.getTermo());
                    }
					obtemSimbolo();
					if (isTermo(")")) {
						if (oper == "read") {
							code(oper, "", "", simbolo.getTermo());
						} else {
							code(oper, simbolo.getTermo(), "", "");
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
		} else if (isTipo(0)) {
			if (!tabelaSimbolo.containsKey(simbolo.getTermo())){
                throw new RuntimeException("Erro semântico! identificador não foi declarado " + simbolo.getTermo());
            }
			obtemSimbolo();
			if (isTermo(":=")) {
				obtemSimbolo();
				String expressaoD = expressao();
				code(":=", expressaoD, "", simbolo.getTermo());
			} else {
				throw new RuntimeException("Erro Sintático! Esperado ':=' Encontrado" + simbolo.getTermo());
			}
		} else if (isTermo("if")) {
			obtemSimbolo();
			String condicaoD = condicao();
			if (isTermo("then")) {
				code("JF", condicaoD, "{", "");
				obtemSimbolo();
				comandos();
				pfalsa();
				if (isTermo("$")) {
					obtemSimbolo();
				} else {
					throw new RuntimeException("Erro Sintático! Esperado '$' Encontrado" + simbolo.getTermo());
				}
			} else {
				throw new RuntimeException("Erro Sintático! Esperado 'then' Encontrado" + simbolo.getTermo());
			}
		} else {
			throw new RuntimeException("Erro Sintático! Esperado 'if' Encontrado" + simbolo.getTermo());
		}
	}

	private String condicao() {
		System.out.println("condicao");
		String expressaoD = expressao();
		String relacaoD = relacao();
		String expressaoLD = expressao();
		String condicaoD = geraTemp();
		code(relacaoD, expressaoD, expressaoLD, condicaoD);
		return condicaoD;

	}

	private String relacao() {
		System.out.println("relacao");
		if (isTermo("=") || !isTermo("<>") || !isTermo(">=") || !isTermo("<=") || !isTermo(">") || !isTermo("<")) {
			String relacaoD = simbolo.getTermo();
			obtemSimbolo();
			return relacaoD;
		} else {
			throw new RuntimeException("Erro Sintático! Esperado = | <> | >= | <= | > | <  Encontrado" + simbolo.getTermo());
		}

	}

	private String expressao() {
		System.out.println("expressao");
		String termoD = termo();
		String outros_termosD = outros_termos(termoD);
		return outros_termosD;
	}

	private String termo() {
		System.out.println("termo");
		String op_unD = op_un();
		String fatorD = fator();
		String mais_fatoresD = mais_fatores(fatorD);
		return mais_fatoresD;

	}

	private String op_un() {
		
		System.out.println("op_un");
		if (isTermo("-")) {
			String op_unD = simbolo.getTermo();
			obtemSimbolo();
			return op_unD;
		}
		return "";

	}

	private String fator() {
		System.out.println("fator");
		if (isTipo(0) || isTipo(1) || isTipo(4)) {
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
		System.out.println("outros_termos");
		if (isTermo("+") || isTermo("-")) {
			String op_adD = op_ad();
			String termoD = termo();
			String outrosD = geraTemp();
			String outrosLD = outros_termos(termoD);
			code(op_adD, outrosE, outrosLD, outrosD);
			return outrosD;
		}
		return outrosE;
	}

	private String op_ad() {
		System.out.println("op_ad");
		if (isTermo("+") || isTermo("-")) {
			String op_adD = simbolo.getTermo();
			obtemSimbolo();
			return op_adD;
		} else {
			throw new RuntimeException("Erro Sintático! Esperado + ou - Encntrado" + simbolo.getTermo());
		}
	}

	private String mais_fatores(String mais_fatoresE) {
		System.out.println("mais_fatores");
		if (isTermo("*") || isTermo("/")) {
			String op_mulD = op_mul();
			String fatorD = fator();
			String mais_fatoresD = geraTemp();
			String mais_fatoresLD = mais_fatores(fatorD);
			code(op_mulD, mais_fatoresE, mais_fatoresLD, mais_fatoresD);
			return mais_fatoresD;
		}
		return mais_fatoresE;

	}

	private String op_mul() {
		System.out.println("op_mul");
		if (isTermo("*") || isTermo("/")) {
			String op_mulD = simbolo.getTermo();
			obtemSimbolo();
			return op_mulD;
		} else {
			throw new RuntimeException("Erro Sintático! Esperado * ou / Encontrado" + simbolo.getTermo());
		}
	}

	private void pfalsa() {
		System.out.println("pfalsa");
		if (isTermo("else")) {
			code("goto", "&", "", "");
			obtemSimbolo();
			comandos();
		}

	}
}
