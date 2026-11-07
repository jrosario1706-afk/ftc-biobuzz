package org.jhoandri.pathing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PathChain {
    private final List<BezierLine> paths = new ArrayList<>();

    public void addPath(BezierLine line) {
        if (line == null) {
            throw new IllegalArgumentException("El path no puede ser null");
        }
        paths.add(line);
    }

    public int size() {
        return paths.size();
    }

    public BezierLine getPath(int index) {
        return paths.get(index);
    }

    public List<BezierLine> getPaths() {
        return Collections.unmodifiableList(paths);
    }
}
