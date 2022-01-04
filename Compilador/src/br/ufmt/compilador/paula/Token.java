package br.ufmt.compilador.paula;

import java.util.HashMap;
import java.util.Map;

public class Token {

	public static final int IDENT = 0;
	public static final int NUMERO = 1;
	public static final int SIMBOL = 2;
	public static final int REAL = 3;
	public static final int RESERVADA = 4;

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
		Map<Integer, String> tipos = new HashMap<Integer, String>();
		tipos.put(0, "Identificador");
		tipos.put(1, "Número");
		tipos.put(2, "Símbolo");
		tipos.put(3, "Real");
		tipos.put(4, "Reservada");

		return "Token [" + tipos.get(tipo) + ", " + termo + "]";
	}
}