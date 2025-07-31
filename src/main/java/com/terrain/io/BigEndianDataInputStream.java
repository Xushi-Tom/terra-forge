package com.terrain.io;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.InputStream;


public class BigEndianDataInputStream extends DataInputStream implements DataInput {

    public BigEndianDataInputStream(InputStream in) {
        super(in);
    }

}
