package br.edu.cefsa.analisadorJquery;

public class Token
{
    String value;
    Integer positionStart;
    String type;

    public Token(String value, String type, Integer positionStart)
    {
        this.value = value;
        this.type = type;
        this.positionStart = positionStart;
    }
}
