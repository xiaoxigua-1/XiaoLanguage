/*
 * This Library is xiao language lexer
 */
package xiao.language.lexer

import xiao.language.utilities.*
import xiao.language.utilities.exceptions.EOFException

class Lexer(
    val fileStream: FileStream
): Iterator<Token> {
    private var isEOF = false

    override fun hasNext(): Boolean = !isEOF

    override fun next(): Token {
        val token = nextToken()

        return token ?: run {
            isEOF = true
            Token(Tokens.EOF, '\u0000', Span(fileStream.getIndex()))
        }
    }
}

private fun Lexer.nextToken(): Token? {
    return if (fileStream.hasNext()) {
        val c = fileStream.next()

        when {
            c == '\n' -> Token(Tokens.NewLine, c, Span(fileStream.getIndex()))
            c == '"' || c == '\'' -> literal(fileStream.getIndex(), c)
            c == '#' -> rawLiteral(fileStream.getIndex())
            c in '0'..'9' -> number(fileStream.getIndex(), c)
            c.isWhitespace() -> whitespace(fileStream.getIndex())
            c.isAsciiSymbol() && c != '_' -> Token(Tokens.Symbol, c, Span(fileStream.getIndex()))
            else -> ident(fileStream.getIndex(), c)
        }
    } else {
        null
    }
}

private fun Lexer.whitespace(start: Int): Token {
    var value = fileStream.current.toString()

    do {
        val c = fileStream.peek()
        when {
            c?.isWhitespace() ?: false -> value += fileStream.next()
            else -> break
        }
    } while(fileStream.hasNext())

    return Token(Tokens.Whitespace, value, Span(start, fileStream.getIndex()))
}

private fun Lexer.literal(start: Int, startChar: Char): Token {
    var value = ""

    for (c in fileStream) {
        value += when(c) {
            startChar -> return Token(Tokens.Literal, value, Span(start, fileStream.getIndex()))
            '\\' -> if (fileStream.hasNext()) fileStream.next().asEscaped() else break
            '\n' -> break
            else -> c
        }
    }

    throw EOFException("Unterminated string", Span(start))
}

private fun Lexer.rawLiteral(start: Int): Token {
    var value = ""

    for (c in fileStream) {
        value += when(c) {
            '#' -> return Token(Tokens.RawLiteral, value, Span(start, fileStream.getIndex()))
            else -> c
        }
    }

    throw EOFException("Unterminated raw string", Span(start))
}

private fun Lexer.number(start: Int, startChar: Char): Token {
    var value = "$startChar"

    return when (startChar) {
        '0' -> Token(Tokens.Number, value, Span(start, fileStream.getIndex()))
        else -> {
            do {
                when (fileStream.peek()) {
                    in '0'..'9' -> value += fileStream.next()
                    else -> break
                }
            } while(fileStream.hasNext())

            Token(Tokens.Number, value, Span(start, fileStream.getIndex()))
        }
    }
}

private fun Lexer.ident(start: Int, startChar: Char): Token {
    var value = "$startChar"

    do {
        val c = fileStream.peek()
        value += when {
            (c?.isWhitespace() ?: false || c?.isAsciiSymbol() ?: false) && c != '_' -> break
            else -> if (fileStream.hasNext()) fileStream.next() else break
        }
    } while(fileStream.hasNext())

    return Token(Tokens.Identifier, value, Span(start, fileStream.getIndex()))
}