/*
 * Copyright (c) 2013, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.graalvm.visualizer.source.impl.ui;

import org.graalvm.visualizer.source.Location;
import org.graalvm.visualizer.source.ui.Trackable;
import org.netbeans.api.actions.Openable;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.api.editor.document.EditorDocumentUtils;
import org.openide.awt.StatusDisplayer;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.text.Line;
import org.openide.util.NbBundle;

import javax.swing.SwingUtilities;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

/**
 *
 */
public class LocationOpener implements Openable, Trackable {
    private final Location location;

    public LocationOpener(Location location) {
        this.location = location;
    }

    @NbBundle.Messages({
            "ERR_LineNotFound=Referenced line does not exist"
    })
    @Override
    public void open() {
        openOrView(true);
    }

    private void openOrView(boolean focus) {
        FileObject toOpen;
        toOpen = location.getOriginFile();
        if (toOpen == null) {
            return;
        }
        int line = location.getLine();
        EditorCookie cake = toOpen.getLookup().lookup(EditorCookie.class);

        Line l;
        try {
            l = cake.getLineSet().getOriginal(line - 1);
        } catch (IndexOutOfBoundsException ex) {
            // expected, the source has changed
            return;
        }
        if (l == null) {
            cake.open();
            StatusDisplayer.getDefault().setStatusText(Bundle.ERR_LineNotFound());
            return;
        }
        if (SwingUtilities.isEventDispatchThread()) {
            l.show(Line.ShowOpenType.REUSE, focus ? Line.ShowVisibilityType.FRONT : Line.ShowVisibilityType.FRONT);
        } else {
            SwingUtilities.invokeLater(() -> l.show(Line.ShowOpenType.REUSE, focus ? Line.ShowVisibilityType.FRONT : Line.ShowVisibilityType.FRONT));
        }
    }

    @Override
    public void viewIfOpened() {
        FileObject toOpen;
        toOpen = location.getOriginFile();
        if (toOpen == null) {
            return;
        }
        for (JTextComponent comp : EditorRegistry.componentList()) {
            Document doc = comp.getDocument();
            FileObject fo = EditorDocumentUtils.getFileObject(doc);
            if (toOpen == fo) {
                view();
            }
        }
    }

    @Override
    public void view() {
        openOrView(false);
    }
}
