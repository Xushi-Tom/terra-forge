package com.terrain.manager;

import com.terrain.io.BigEndianDataInputStream;
import com.terrain.io.BigEndianDataOutputStream;
import com.utils.TileWgs84Utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Getter
@Setter
@Slf4j
@AllArgsConstructor
@NoArgsConstructor
public class TileIndices {

    private int X = 0;
    private int Y = 0;
    private int L = 0; // tile depth.

    public void set(int x, int y, int l) {
        X = x;
        Y = y;
        L = l;
    }

    public void copyFrom(TileIndices tileIndices) {
        X = tileIndices.X;
        Y = tileIndices.Y;
        L = tileIndices.L;
    }

    public void saveDataOutputStream(BigEndianDataOutputStream dataOutputStream) {
        try {
            dataOutputStream.writeInt(X);
            dataOutputStream.writeInt(Y);
            dataOutputStream.writeInt(L);

        } catch (Exception e) {
            log.error("Error:", e);
        }
    }

    public void loadDataInputStream(BigEndianDataInputStream dataInputStream) throws IOException {
        X = dataInputStream.readInt();
        Y = dataInputStream.readInt();
        L = dataInputStream.readInt();
    }

    public boolean isValid() {

        return TileWgs84Utils.isValidTileIndices(L, X, Y);
    }

    public TileIndices getDownTileIndices(boolean originIsLeftUp) {
        TileIndices tileIndices = new TileIndices();
        if (originIsLeftUp) {
            tileIndices.set(X, Y + 1, L);
        } else {
            tileIndices.set(X, Y - 1, L);
        }

        return tileIndices;
    }

    public TileIndices getRightTileIndices(boolean originIsLeftUp) {
        TileIndices tileIndices = new TileIndices();
        if (originIsLeftUp) {
            tileIndices.set(X + 1, Y, L);
        } else {
            tileIndices.set(X + 1, Y, L);
        }

        return tileIndices;
    }

    public TileIndices getUpTileIndices(boolean originIsLeftUp) {
        TileIndices tileIndices = new TileIndices();
        if (originIsLeftUp) {
            tileIndices.set(X, Y - 1, L);
        } else {
            tileIndices.set(X, Y + 1, L);
        }

        return tileIndices;
    }

    public TileIndices getLeftTileIndices(boolean originIsLeftUp) {
        TileIndices tileIndices = new TileIndices();
        if (originIsLeftUp) {
            tileIndices.set(X - 1, Y, L);
        } else {
            tileIndices.set(X - 1, Y, L);
        }

        return tileIndices;
    }

    public TileIndices getChildLeftUpTileIndices(boolean originIsLeftUp) {
        TileIndices tileIndices = new TileIndices();
        if (originIsLeftUp) {
            tileIndices.set(X * 2, Y * 2, L + 1);
        } else {
            tileIndices.set(X * 2, Y * 2 + 1, L + 1);
        }

        return tileIndices;
    }

    public TileIndices getChildRightUpTileIndices(boolean originIsLeftUp) {
        TileIndices tileIndices = new TileIndices();
        if (originIsLeftUp) {
            tileIndices.set(X * 2 + 1, Y * 2, L + 1);
        } else {
            tileIndices.set(X * 2 + 1, Y * 2 + 1, L + 1);
        }

        return tileIndices;
    }

    public TileIndices getChildLeftDownTileIndices(boolean originIsLeftUp) {
        TileIndices tileIndices = new TileIndices();
        if (originIsLeftUp) {
            tileIndices.set(X * 2, Y * 2 + 1, L + 1);
        } else {
            tileIndices.set(X * 2, Y * 2, L + 1);
        }

        return tileIndices;
    }

    public TileIndices getChildRightDownTileIndices(boolean originIsLeftUp) {
        TileIndices tileIndices = new TileIndices();
        if (originIsLeftUp) {
            tileIndices.set(X * 2 + 1, Y * 2 + 1, L + 1);
        } else {
            tileIndices.set(X * 2 + 1, Y * 2, L + 1);
        }

        return tileIndices;
    }

    public String getString() {
        String result = "";
        result += "X : " + X + ", Y : " + Y + ", L : " + L;
        return result;
    }

}
