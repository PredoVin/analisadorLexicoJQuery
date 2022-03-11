package br.edu.cefsa.analisadorJquery;

import javax.sound.sampled.Line;
import javax.swing.*;
import javax.swing.table.TableModel;
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

            List<Token> tokenList = lexArchive(compressedJquery);

            List<List<String>> tabelaLex = new ArrayList<>();

            for(Token token : tokenList)
            {
                tabelaLex.get(0).add(token.positionStart.toString());
                tabelaLex.get(1).add(token.value);
                tabelaLex.get(2).add(token.type);
            }

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
                                                     "(", "Function",
                                                     ")", "Parameter",
                                                     "{", "Object",
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

        List<String> operators = Arrays.asList("!","=","+","-","*","/","%","&","|");

        Pattern patt = Pattern.compile("[^a-z0-9]", Pattern.CASE_INSENSITIVE);
        Pattern numPatt = Pattern.compile("[0-9]", Pattern.CASE_INSENSITIVE);

        char[] jQueryArray = compressedJquery.toCharArray();
        String word = "";
        boolean openedQuotation = false; // identifica que palavra está entre aspas
        boolean isToken = false;

        while(index < compressedJquery.length())
        {
            char thisChar = jQueryArray[index];

            if(operators.contains(thisChar))
            {
                tokenList.add(new Token(word, "Operator", wordStart, index-1));
            }

            if(patt.matcher(String.valueOf(thisChar)).find()) //Se atende ao RegEx
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
                    tokenList.add(new Token(word, "KeyWord", wordStart, index - 1));
                    isToken = true;
                } else if (possibleWordEnds.containsKey(String.valueOf(thisChar))) {
                    if (!word.isBlank()) {
                        if (numPatt.matcher(word).find() && !openedQuotation) {
                            if (thisChar == '.') {
                                word += thisChar;
                            } else //Integer
                            {
                                tokenList.add(new Token(word, "Integer", wordStart, index - 1));
                                isToken = true;
                            }
                        } else if (word.contains(".") && numPatt.matcher(word.replace(".", "")).find() && !openedQuotation) {
                            if (numPatt.matcher(word.replace(".", "")).find()) //Se removendo . sobram numeros
                            {
                                tokenList.add(new Token(word, "Float", wordStart, index - 1));
                                isToken = true;
                            } else {
                                tokenList.add(new Token(word, "Object", wordStart, index - 1));
                                isToken = true;
                            }
                        } else {

                            if (thisChar == '"' || thisChar == '\'') {
                                openedQuotation = false;
                            }

                            tokenList.add(new Token(word, possibleWordEnds.get(String.valueOf(thisChar)), wordStart, index - 1));
                            isToken = true;
                        }
                    }
                    else if (thisChar == '"' || thisChar == '\'') {
                        openedQuotation = true;
                    }
                }
            }

            index++;
            if(isToken) {
                word = "";
                isToken = false;
            }
        }

        return tokenList;
    }
}
