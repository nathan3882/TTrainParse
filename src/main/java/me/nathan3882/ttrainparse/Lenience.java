/*
 *
 *  * Copyright (c) 2019 Nathan James Allanson
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  * of this software and associated documentation files (the "Software"), to deal
 *  * in the Software. Permissions granted granting right to use, copy, modify, merge,
 *  * publish and distribute subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all
 *  * copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *
 *
 */

package me.nathan3882.ttrainparse;

public class Lenience {

    private final int occurencesStartCheck = 400;
    private final int occurencesDecreaseBy = 30;
    private final double responseSpecificity = .30;

    public int getOccurencesStartCheck() {
        return occurencesStartCheck;
    }

    public int getOccurencesDecreaseBy() {
        return occurencesDecreaseBy;
    }

    public double getResponseSpecificity() {
        return responseSpecificity;
    }
}
