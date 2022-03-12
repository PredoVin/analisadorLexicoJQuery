package br.edu.cefsa.analisadorJquery;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Pattern;

public class Main {

    public static void main(String[] args)
    {
        try
        {
            String compressedJquery;

            URL jqueryURL = new URL("https://code.jquery.com/jquery-3.6.0.js");
            Scanner fileReader = new Scanner(jqueryURL.openStream());

            FileWriter fileWriter = new FileWriter("jquery-compressed-3.6.0.js");
            PrintWriter printWriter = new PrintWriter(fileWriter);

            FileWriter fileWriterLex = new FileWriter("tabelaLex.Lex");
            PrintWriter printWriterLex = new PrintWriter(fileWriterLex);

            compressedJquery = compressao(fileReader);
            printWriter.print(compressedJquery);
            printWriter.close();

            List<Token> tokenList = lexArchive(compressedJquery);

            printWriterLex.println("Posição,Valor,Tipo");
            System.out.println("Posição,Valor,Tipo");
            for(Token token : tokenList)
            {
                printWriterLex.println(token.positionStart + "," + token.value + "," + token.type);
                System.out.println(token.positionStart + "," + token.value + "," + token.type);
            }

            printWriterLex.close();

            System.out.println("End of Program");
        }
        catch(Exception error)
        {
            System.out.println("error => " + error.toString());
            System.out.println("line => " + error.getStackTrace()[0].getLineNumber());
            System.out.println("error message => " + error.getStackTrace()[0].toString());
            System.out.println("error cause => " + error.getCause());
        }
    }

    public static String compressao(Scanner jQuery)
    {
        boolean inCommentary = false;
        String compressedJquery = "";
        while(jQuery.hasNextLine())
        {
            String thisLine = jQuery.nextLine();
            boolean singleLineComment = thisLine.contains("//");
            boolean commentOpener = thisLine.contains("/*");
            boolean commentCloser = thisLine.contains("*/");
            int openerPosition = thisLine.indexOf("/*");
            int closerPosition = thisLine.indexOf("*/");

            //Remocao de comentarios
            if(singleLineComment)
            {
                thisLine = thisLine.substring(0,thisLine.indexOf("//"));
            }
            if(commentOpener && commentCloser)
            {
                String removedRegion = thisLine.substring(openerPosition, closerPosition);
                thisLine = thisLine.replace(removedRegion, "");
            }
            else if(commentOpener && !commentCloser)
            {
                thisLine = "";
                inCommentary = true;
            }
            else if(!commentOpener && commentCloser)
            {
                thisLine = thisLine.substring(closerPosition+2);
                inCommentary = false;
            }
            if(inCommentary)
            {
                thisLine = "";
            }

            //Compressao
            thisLine = thisLine.trim();
            thisLine = thisLine.replaceAll("\t", "");
            thisLine = thisLine.replaceAll("\n", "");
            thisLine = thisLine.replaceAll("\r", "");
            thisLine = thisLine.replaceAll(System.getProperty("line.separator"), "");

            //Registro
            compressedJquery += thisLine;
        }

        return compressedJquery;
    }

    public static List<Token> lexArchive(String compressedJquery)
    {
        List<Token> tokenList = new ArrayList<Token>();
        List<String> jsTokens = Arrays.asList("break", "case", "catch", "class", "const", "continue",
                "debugger", "default", "delete", "do", "else", "export", "extends", "finally",
                "for", "function", "if", "import", "in", "instanceof", "new", "return",
                "super", "switch", "this", "throw", "try", "typeof", "var", "void", "while", "with", "yield", "enum",
                "implements", "interface", "let", "package", "private", "private", "protected", "public",
                "static", "await", "abstract", "boolean", "byte", "char", "double", "final", "float",
                "goto", "int", "long", "native", "short", "synchronized", "throws", "transient", "volatile",
                "null", "true", "false", "undefined");

        Map<String, String> possibleWordEnds = new HashMap<String, String>();
        possibleWordEnds.put(" ", "Variable");
        possibleWordEnds.put(".", "ObjectName");
        possibleWordEnds.put(";", "Variable");
        possibleWordEnds.put("(", "Function");
        possibleWordEnds.put(")", "Parameter");
        possibleWordEnds.put("{", "Object");
        possibleWordEnds.put("}", "Variable");
        possibleWordEnds.put("[", "Vector");
        possibleWordEnds.put("]", "Variable");
        possibleWordEnds.put("=", "Variable");
        possibleWordEnds.put("\"","String");
        possibleWordEnds.put("'", "Char");
        possibleWordEnds.put("!", "Variable");
        possibleWordEnds.put("+", "Variable");
        possibleWordEnds.put("-", "Variable");
        possibleWordEnds.put("*", "Variable");
        possibleWordEnds.put("/", "Variable");
        possibleWordEnds.put("%", "Variable");
        possibleWordEnds.put(":", "Variable");
        possibleWordEnds.put("<", "Variable");
        possibleWordEnds.put(">", "Variable");

        List<String> operators = Arrays.asList("!","=","+","-","*","/","%","&","|","<",">");

        Pattern patt = Pattern.compile("[^a-z0-9]", Pattern.CASE_INSENSITIVE);
        Pattern numPatt = Pattern.compile("[0-9]", Pattern.CASE_INSENSITIVE);

        char[] jQueryArray = compressedJquery.toCharArray();
        String word = "";
        boolean openedQuotation = false; // identifica que palavra está entre aspas
        boolean isToken = false;
        int index = 0;
        int wordStart = 0;

        while(index < jQueryArray.length)
        {
            char thisChar = jQueryArray[index];

            if(operators.contains(String.valueOf(thisChar)) && !openedQuotation)
            {
                tokenList.add(new Token(String.valueOf(thisChar), "Operator", index));
            }

            if(!patt.matcher(String.valueOf(thisChar)).find()) //Se não é caractere especial
            {
                if(word.isBlank())
                {
                    wordStart = index;
                }
                word += thisChar; //Preenche string Word
            }
            else
            {
                if (jsTokens.contains(word) && !openedQuotation)//Palavra reservada
                {
                    tokenList.add(new Token(word, "KeyWord", wordStart));
                    word = "";
                }
                else if(possibleWordEnds.containsKey(String.valueOf(thisChar)))
                {
                    if(!word.isBlank())
                    {
                        if(numPatt.matcher(word).find() && !openedQuotation)//Se apenas números
                        {
                            if(thisChar == '.')
                            {
                                word += thisChar;
                            } else //Integer
                            {
                                tokenList.add(new Token(word, "Integer", wordStart));
                                word = "";
                            }
                        }
                        else if(word.contains(".")  && !openedQuotation)
                        {
                            if(numPatt.matcher(word.replace(".","")).find()) //Se removendo . sobram numeros
                            {
                                tokenList.add(new Token(word, "Float", wordStart));
                                word = "";
                            }
                            else
                            {
                                tokenList.add(new Token(word, "Object", wordStart));
                                word = "";
                            }
                        }
                        else
                        {
                            if (thisChar == '"' || thisChar == '\'')
                            {
                                openedQuotation = false;
                            }
                            tokenList.add(new Token(word, possibleWordEnds.get(String.valueOf(thisChar)), wordStart));
                            word = "";
                        }
                    }
                    else if (thisChar == '"' || thisChar == '\'')
                    {
                        openedQuotation = true;
                    }
                }
            }
            index++;
        }

        return tokenList;
    }
}
