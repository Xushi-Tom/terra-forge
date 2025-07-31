package com.terrain.io;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.OutputStream;

/**
 * BigEndianDataOutputStream
 */
public class BigEndianDataOutputStream extends DataOutputStream implements DataOutput {

    public BigEndianDataOutputStream(OutputStream out) {
        super(out);
    }

}
