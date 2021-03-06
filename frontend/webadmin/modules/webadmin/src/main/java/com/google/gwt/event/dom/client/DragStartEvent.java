/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.event.dom.client;

/**
 * Represents a native drag start event.
 */
public class DragStartEvent extends DragDropEventBase<DragStartHandler> {

    /**
     * Event type for drag start events. Represents the meta-data associated with this event.
     */
    private static final Type<DragStartHandler> TYPE = new Type<DragStartHandler>("dragstart", //$NON-NLS-1$
            new DragStartEvent());

    /**
     * Gets the event type associated with drag start events.
     *
     * @return the handler type
     */
    public static Type<DragStartHandler> getType() {
        return TYPE;
    }

    /**
     * Protected constructor, use
     * {@link DomEvent#fireNativeEvent(com.google.gwt.dom.client.NativeEvent, com.google.gwt.event.shared.HasHandlers)}
     * or
     * {@link DomEvent#fireNativeEvent(com.google.gwt.dom.client.NativeEvent, com.google.gwt.event.shared.HasHandlers, com.google.gwt.dom.client.Element)}
     * to fire drag start events.
     */
    protected DragStartEvent() {
    }

    @Override
    public final Type<DragStartHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(DragStartHandler handler) {
        handler.onDragStart(this);
    }

}
