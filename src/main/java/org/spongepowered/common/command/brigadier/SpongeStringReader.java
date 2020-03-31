/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.command.brigadier;

import static org.spongepowered.common.util.SpongeCommonTranslationHelper.t;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.command.exception.ArgumentParseException;
import org.spongepowered.api.command.parameter.ArgumentReader;
import org.spongepowered.api.text.Text;
import org.spongepowered.common.command.brigadier.context.SpongeCommandContextBuilder;

// ArgumentReader.Mutable specifies a non null getRead() method, StringReader suggests its
// nullable - but it isn't. So we just need to suppress the warning.
@SuppressWarnings("NullableProblems")
public class SpongeStringReader extends StringReader implements ArgumentReader.Mutable {

    private static final char SYNTAX_QUOTE = '"';

    public SpongeStringReader(final String string) {
        super(string);
    }

    public SpongeStringReader(final StringReader other) {
        super(other);
    }

    @Override
    @NonNull
    public String getInput() {
        return this.getString();
    }

    @Override
    public char parseChar() {
        return this.read();
    }

    @Override
    public int parseInt() throws ArgumentParseException {
        try {
            return this.readInt();
        } catch (final CommandSyntaxException e) {
            throw new ArgumentParseException(t("Could not parse an integer"), e, this.getString(), this.getCursor());
        }
    }

    @Override
    public double parseDouble() throws ArgumentParseException {
        try {
            return this.readDouble();
        } catch (final CommandSyntaxException e) {
            throw new ArgumentParseException(t("Could not parse a double"), e, this.getString(), this.getCursor());
        }
    }

    @Override
    public float parseFloat() throws ArgumentParseException {
        try {
            return this.readFloat();
        } catch (final CommandSyntaxException e) {
            throw new ArgumentParseException(t("Could not parse a float"), e, this.getString(), this.getCursor());
        }
    }

    @Override
    @NonNull
    public String parseUnquotedString() {
        final int start = this.getCursor();
        while (this.canRead() && !Character.isWhitespace(this.peek())) {
            this.skip();
        }
        return this.getString().substring(start, this.getCursor());
    }

    @Override
    @NonNull
    public String parseString() throws ArgumentParseException {
        try {
            if (this.canRead() && this.peek() == SYNTAX_QUOTE) {
                return this.readQuotedString();
            } else {
                return this.readUnquotedString();
            }
        } catch (final CommandSyntaxException e) {
            throw new ArgumentParseException(t("Could not parse string"), e, this.getString(), this.getCursor());
        }
    }

    @Override
    public boolean parseBoolean() throws ArgumentParseException {
        try {
            return this.readBoolean();
        } catch (final CommandSyntaxException e) {
            throw new ArgumentParseException(t("Could not parse a boolean"), e, this.getString(), this.getCursor());
        }
    }

    @Override
    @NonNull
    public SpongeImmutableArgumentReader getImmutable() {
        return new SpongeImmutableArgumentReader(this.getString(), this.getCursor());
    }

    @Override
    public void setState(@NonNull final ArgumentReader state) throws IllegalArgumentException {
        if (state.getInput().equals(this.getString())) {
            this.setCursor(state.getCursor());
        } else {
            throw new IllegalArgumentException("The provided ArgumentReader does not match this ArgumentReader");
        }
    }

    @Override
    @NonNull
    public ArgumentParseException createException(@NonNull final Text errorMessage) {
        return new ArgumentParseException(errorMessage, this.getInput(), this.getCursor());
    }

}
