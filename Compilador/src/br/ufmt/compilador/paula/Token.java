package br.ufmt.compilador.paula;

public class Token {

	public static final int IDENT = 0;
	public static final int NUMERO = 1;
	public static final int ESPACO = 2;
	public static final int SIMBOL = 3;
	public static final int REAL = 4;
	public static final int RESERVADA = 5;

	private int tipo;
	private String termo;

	public int getTipo() {
		return this.tipo;
	}

	public void setTipo(int tipo) {
		this.tipo = tipo;
	}

	public String getTermo() {
		return this.termo;
	}

	public void setTermo(String termo) {
		this.termo = termo;
	}

	@Override
	public String toString() {
		return "Token [" + tipo + ", " + termo + "]";
	}
}