package br.ufmt.compilador.paula;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class LexScanner {

	private char[] conteudo;
	private int estado;
	private int pos;

	public LexScanner(String arq) {
		try {
			byte[] bytes = Files.readAllBytes(Paths.get(arq));
			conteudo = (new String(bytes)).toCharArray();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Token nextToken() {
		if (isEOF()) {
			return null;
		}
		estado = 0;
		char c;
		Token token = null;
		String termo = "";
		while (true) {
			if (isEOF()) {
				pos = conteudo.length + 1;
			}
			c = nextChar();
			switch (estado) {
			case 0:
				if (isLetra(c)) {
					termo += c;
					estado = 1;
				} else if (isDigito(c)) {
					termo += c;
					estado = 3;
				} else if (isEspaco(c)) {
					estado = 0;
				} else if (isOperator(c)) {
					termo += c;
					estado = 5;
				} else if (isOperator2(c)) {
					termo += c;
					estado = 8;
				} else {
			          if (c == 0) {
			              return null;
			            }
			            termo += c;
			            throw new RuntimeException("Token não não reconhecido: " + c);
			          }
				break;
			case 1:
				if (isLetra(c) || isDigito(c)) {
					termo += c;
					estado = 1;
				} else if (isPalavraR(termo)) {
					token = new Token();
					token.setTipo(Token.RESERVADA);
					token.setTermo(termo);
					back();
					return token;
				} else if (isOperator2(c)) {
					back();
					token = new Token();
					token.setTipo(Token.IDENT);
					token.setTermo(termo);
					return token;
				} else if (!isLetra(c) || !isDigito(c)) {
					estado = 2;
				}
				break;
			case 2:
				back();
				token = new Token();
				token.setTipo(Token.IDENT);
				token.setTermo(termo);
				return token;

			case 3:
				if (isDigito(c)) {
					estado = 3;
					termo += c;
				} else if (c == '.') {
					estado = 9;
					termo += c;
				} else if (isOperator2(c)) {
					back();
					token = new Token();
					token.setTipo(Token.NUMERO);
					token.setTermo(termo);
					return token;
				} else if (!isLetra(c)) {
					estado = 4;
				} else {
					throw new RuntimeException("Número não reconhecido!");
				}
				break;
			case 4:
				back();
				token = new Token();
				token.setTipo(Token.NUMERO);
				token.setTermo(termo);
				return token;
			case 5:
				if (c == '=') {
					estado = 8;
					termo += c;
				} else if (!isOperator(c) && !isOperator2(c)) {
					estado = 8;
				} else {
					throw new RuntimeException("Símbolo não reconhecido!");
				}
				break;
			case 8:
				token = new Token();
				token.setTipo(Token.SIMBOL);
				token.setTermo(termo);
				back();
				return token;
			case 9:
				if (isDigito(c)) {
					estado = 9;
					termo += c;
				} else if (!isLetra(c)) {
					token = new Token();
					token.setTipo(Token.REAL);
					token.setTermo(termo);
					back();
					return token;
				} else {
					throw new RuntimeException("Número não reconhecido!");
				}
				break;
			}
		}
	}

	private boolean isLetra(char c) {
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
	}

	private boolean isDigito(char c) {
		return c >= '0' && c <= '9';
	}

	private boolean isOperator(char c) {
		return c == '>' || c == '<' || c == '=' || c == '!' || c == ':';
	}

	private boolean isOperator2(char c) {
		return c == '+' || c == '-' || c == '*' || c == '/' || c == '(' || c == ')' || c == ',' || c == ';' || c == '.'
				|| c == '$';
	}

	private boolean isEspaco(char c) {
		return c == ' ' || c == '\n' || c == '\t' || c == '\r';
	}

	private boolean isPalavraR(String c) {
		// return c == "if" || c == "then" || c == "else" || c == "write" || c ==
		// "begin" || c == "end";
		String palavra[] = { "begin", "end", "program", "write", "read", "if", "else", "then", "real", "integer" };
		for (int i = 0; i < 10; i++) {
			if (c.matches(palavra[i])) {
				return true;
			}
		}
		return false;
	}

	private boolean isEOF() {
		return pos >= conteudo.length;
	}

	private char nextChar() {
		if (isEOF()) {
			return 0;
		}
		return conteudo[pos++];
	}

	private void back() {
		pos--;
	}
}
