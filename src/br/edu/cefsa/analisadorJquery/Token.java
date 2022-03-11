package br.edu.cefsa.analisadorJquery;

public class Token
{
    String value;
    Double positionStart;
    Double positionEnd;
    String type;

    public Token(String value, String type, double positionStart, double positionEnd)
    {
        this.value = value;
        this.type = type;
        this.positionStart = positionStart;
        this.positionEnd = positionEnd;
    }
}
