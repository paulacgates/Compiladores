package br.ufmt.compilador.paula;

public class Simbolo {
	private String nome;
	private int tipo;
	private int endRel;
	private int primInstr;
	
	public Simbolo(int tipo, String nome, int endRel){
	    this.nome = nome;
	    this.tipo = tipo;
	    this.endRel = endRel;
		this.primInstr = -1;
	}

	public Simbolo(int tipo, String nome, int primInstr, int endRel){
		this.nome = nome;
		this.tipo = tipo;
		this.primInstr = primInstr;
		this.endRel = endRel;
	}

	  public String getNome() {
	    return this.nome;
	  }

	  public void setNome(String nome) {
	    this.nome = nome;
	  }

	  public int getTipo() {
	    return this.tipo;
	  }

	  public void setTipo(int tipo) {
	    this.tipo = tipo;
	  }

	public int getEndRel() {
		return this.endRel;
	}

	public void setEndRel(int endRel) {
		this.endRel = endRel;
	}

	public int getPrimInstr() {
		return this.primInstr;
	}

	public void setPrimInstr(int primInstr) {
		this.primInstr = primInstr;
	}
}
