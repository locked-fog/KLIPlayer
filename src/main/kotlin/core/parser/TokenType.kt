package com.lockedfog.kliplayer.core.parser

enum class TokenType {
    LEFT_BRACKET,  //[
    RIGHT_BRACKET, //]
    TIME_ABS,      //Absolute time
    TIME_REL,      //Relative time
    KEYWORD,       //keyword
    IDENTIFIER,    //identifier
    OPERATOR,      // +, -, etc.
    NUMBER,        //123.456
    STRING,        //"string"
    TEXT,           //text
    COLOR,          //color code rrggbb/RRGGBB
    NEWLINE,        //\n
    EOF             //end of file
}