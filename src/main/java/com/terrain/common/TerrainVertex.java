package com.terrain.common;

import com.terrain.enums.TerrainObjectStatus;
import com.terrain.io.BigEndianDataInputStream;
import com.terrain.io.BigEndianDataOutputStream;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Slf4j
public class TerrainVertex {
    private TerrainHalfEdge outingHEdge = null;
    private Vector3d position = new Vector3d();
    private Vector3f normal = null;
    private int id = -1;
    private int outingHEdgeId = -1;
    private TerrainObjectStatus objectStatus = TerrainObjectStatus.ACTIVE;

    public void deleteObjects() {
        outingHEdge = null;
        position = null;
    }

    public boolean isCoincidentVertexXY(TerrainVertex vertex, double error) {
        if (vertex == null) {
            return false;
        }

        return Math.abs(this.getPosition().x - vertex.getPosition().x) < error && Math.abs(this.getPosition().y - vertex.getPosition().y) < error;
    }

    public void calculateNormal() {
        if (this.normal == null) {
            this.normal = new Vector3f();
        }

        this.normal.set(0, 0, 0);
        List<TerrainHalfEdge> outingHalfEdges = this.getAllOutingHalfEdges();
        for (TerrainHalfEdge outingHalfEdge : outingHalfEdges) {
            TerrainTriangle triangle = outingHalfEdge.getTriangle();
            if (triangle == null) {
                continue;
            }

            Vector3f normal = triangle.getNormal();
            if (normal == null) {
                continue;
            }

            this.normal.add(normal);
        }

        // if this vertex has no normal, then set default normal
        if (this.normal.equals(0, 0, 0)) {
            log.warn("This vertex has no normal. id : {}", this.id);
            this.normal.set(0, 0, 1);
        }

        this.normal.normalize();
    }

    public List<TerrainHalfEdge> getAllOutingHalfEdges() {
        List<TerrainHalfEdge> outingHalfEdges = new ArrayList<>();

        // there are 2 cases: this vertex is interior vertex or boundary vertex, but we dont know
        // 1- interior vertex
        // 2- boundary vertex
        if (this.outingHEdge == null) {
            // error
            log.warn("This vertex has no outingHEdge. id : {}", this.id);
        }

        if (this.outingHEdge.getObjectStatus() == TerrainObjectStatus.DELETED) {
            log.warn("This outingHEdge is DELETED. id : {}", this.id);
            return outingHalfEdges;
        }

        TerrainHalfEdge firstHalfEdge = this.outingHEdge;
        TerrainHalfEdge currHalfEdge = this.outingHEdge;
        outingHalfEdges.add(this.outingHEdge); // put the first halfEdge
        boolean finished = false;
        boolean isInteriorVertex = true;
        int counter = 0;
        while (!finished) {
            TerrainHalfEdge twinHalfEdge = currHalfEdge.getTwin();
            if (twinHalfEdge == null) {
                finished = true;
                isInteriorVertex = false;
                break;
            }
            TerrainHalfEdge nextHalfEdge = twinHalfEdge.getNext();
            if (nextHalfEdge == null) {
                finished = true;
                isInteriorVertex = false;
                break;
            } else if (nextHalfEdge == firstHalfEdge) {
                finished = true;
                break;
            }

            outingHalfEdges.add(nextHalfEdge);
            currHalfEdge = nextHalfEdge;

            counter++;
            if (counter > 10) {
                log.info("Info : This vertex has more than 10 outing halfEdges. id : " + this.id);
            }
        }

        // if this vertex is NO interior vertex, then must check if there are more outing halfEdges
        if (!isInteriorVertex) {
            // check if there are more outing halfEdges
            currHalfEdge = this.outingHEdge;
            finished = false;
            while (!finished) {
                TerrainHalfEdge prevHalfEdge = currHalfEdge.getPrev();
                if (prevHalfEdge == null || prevHalfEdge.getObjectStatus() == TerrainObjectStatus.DELETED) {
                    finished = true;
                    break;
                }
                TerrainHalfEdge twinHalfEdge = prevHalfEdge.getTwin();
                if (twinHalfEdge == null || twinHalfEdge.getObjectStatus() == TerrainObjectStatus.DELETED) {
                    finished = true;
                    break;
                }
                outingHalfEdges.add(twinHalfEdge);
                if (outingHalfEdges.size() > 4) {
                    break;
                }
                currHalfEdge = twinHalfEdge;
            }
        }
        return outingHalfEdges;
    }


    public void saveDataOutputStream(BigEndianDataOutputStream dataOutputStream) {
        try {
            // First, save id
            dataOutputStream.writeInt(id);
            dataOutputStream.writeDouble(position.x);
            dataOutputStream.writeDouble(position.y);
            dataOutputStream.writeDouble(position.z);

            // 2nd, save outingHEdge
            if (outingHEdge != null) {
                dataOutputStream.writeInt(outingHEdge.getId());
            } else {
                dataOutputStream.writeInt(-1);
            }
        } catch (Exception e) {
            log.error("Error:", e);
        }
    }

    public void loadDataInputStream(BigEndianDataInputStream dataInputStream) throws IOException {
        this.id = dataInputStream.readInt();
        this.getPosition().x = dataInputStream.readDouble();
        this.getPosition().y = dataInputStream.readDouble();
        this.getPosition().z = dataInputStream.readDouble();

        this.outingHEdgeId = dataInputStream.readInt();
    }
}
