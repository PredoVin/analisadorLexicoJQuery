package br.edu.cefsa.analisadorJquery;

import javax.sound.sampled.Line;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
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


            compressedJquery = compressao(fileReader);

            printWriter.print(compressedJquery);
            printWriter.close();

        }
        catch(Exception error)
        {
            System.out.println("error => " + error.toString());
            System.out.println("error message => " + error.getMessage());
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
                "null", "true", "false");

        int index = 0;
        int wordStart = 0;
        Map<String, String> possibleWordEnds = Map.of(" ", "Variable",
                                                     ".", "ObjectName",
                                                     ";", "Value",
                                                     "(", "Method",
                                                     ")", "Parameter",
                                                     "{", "ObjectName",
                                                     "}", "Undefined",
                                                     "[", "Vector",
                                                     "]", "Variable",
                                                    "=", "Variable");
                                    possibleWordEnds.put("\"","String");
                                    possibleWordEnds.put("'","Char");
                                    possibleWordEnds.put("!","Variable");
                                    possibleWordEnds.put("+","Variable");
                                    possibleWordEnds.put("-","Variable");
                                    possibleWordEnds.put("*","Variable");
                                    possibleWordEnds.put("/","Variable");
                                    possibleWordEnds.put("%","Variable");
                                    possibleWordEnds.put(":","Variable");

        Pattern patt = Pattern.compile("[^a-z0-9]", Pattern.CASE_INSENSITIVE);
        Pattern numPatt = Pattern.compile("[0-9]", Pattern.CASE_INSENSITIVE);

        char[] jQueryArray = compressedJquery.toCharArray();
        String word = "";

        while(index < compressedJquery.length())
        {
            if(patt.matcher(String.valueOf(jQueryArray[index])).find()) //Se atende ao RegEx
            {
                if(word.isBlank())
                {
                    wordStart = index;
                }
                word += jQueryArray[index];
            }
            else
            {
                if(jsTokens.contains(word))
                {
                    tokenList.add(new Token(word, "KeyWord", wordStart, index-1));
                }
                else if(possibleWordEnds.containsKey(String.valueOf(jQueryArray[index])))
                {
                    if(!word.isBlank())
                    {
                        if(numPatt.matcher(word).find())
                        {
                            if(!(jQueryArray[index] == '.'))
                            {
                                tokenList.add(new Token(word, "Integer", wordStart, index-1));
                            }
                            else
                            {
                                word += jQueryArray[index];
                            }
                        }
                        else if(word.contains("."))
                        {
                            tokenList.add(new Token(word, "Float", wordStart, index-1));
                        }
                        else
                        {
                            tokenList.add(new Token(word, possibleWordEnds.get(String.valueOf(jQueryArray[index])), wordStart, index-1));
                        }
                    }
                }
                word = "";
            }

        }
    }



}
