/*******************************************************************************
 * Copyright (c) 2025 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 *******************************************************************************/
package org.eclipse.kura.remoteservice.distribution.rest.provider.whiteboard;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.jakartars.whiteboard.JakartarsWhiteboardConstants;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;

@Component(service = { MessageBodyReader.class, MessageBodyWriter.class }, property = {
        JakartarsWhiteboardConstants.JAKARTA_RS_APPLICATION_SELECT + "=(" + JakartarsWhiteboardConstants.JAKARTA_RS_NAME
                + "=" + Constants.APPLICATION_NAME + ")", //
        JakartarsWhiteboardConstants.JAKARTA_RS_EXTENSION + "=true", //
        Constants.KURA_REMOTESERVICES_EXTENSION_PROP_NAME + "=true" })
@Provider
@Produces("*/*")
@Consumes("*/*")
public class JacksonSerializer implements MessageBodyReader<Object>, MessageBodyWriter<Object> {

    final JacksonJsonProvider provider = new JacksonJsonProvider();

    @Override
    public boolean isWriteable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
        return provider.isWriteable(arg0, arg1, arg2, arg3);
    }

    @Override
    public void writeTo(Object arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4,
            MultivaluedMap<String, Object> arg5, OutputStream arg6) throws IOException, WebApplicationException {
        provider.writeTo(arg0, arg1, arg2, arg3, arg4, arg5, arg6);

    }

    @Override
    public boolean isReadable(Class<?> arg0, Type arg1, Annotation[] arg2, MediaType arg3) {
        return provider.isReadable(arg0, arg1, arg2, arg3);
    }

    @Override
    public Object readFrom(Class<Object> arg0, Type arg1, Annotation[] arg2, MediaType arg3,
            MultivaluedMap<String, String> arg4, InputStream arg5) throws IOException, WebApplicationException {
        return provider.readFrom(arg0, arg1, arg2, arg3, arg4, arg5);
    }

}
