/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sigea.main;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.API.Match;
import static io.vavr.Predicates.isIn;

/**
 *
 * @author Pasquale Livecchi
 */
public class DataPrimitiveUtil {

    public static int dataTypeBitSize(String dataType) {
        return Match(dataType).of(
                Case($("U1"), (short) 1),
                Case($("U8"), (short) 8),
                Case($(isIn("U16", "S16")), (short) 16),
                Case($(isIn("U32", "S32", "F32")), (short) 32),
                Case($(isIn("U64", "S64", "F64")), (short) 64),
                Case($(), obj -> {
                    throw new RuntimeException("Unsupported Data Type");
                })
        );
    }

    public static void convertValToPrim(String dataType, double val) {
        Match(dataType).of(
                Case($(isIn("U1", "U8")), (byte) val),
                Case($(isIn("U16", "S16")), (short) val),
                Case($(isIn("U32", "S32")), (int) val),
                Case($("F32"), (float) val),
                Case($(isIn("U64", "S64")), (long) val),
                Case($("F64"), (double) val));
    }
}
