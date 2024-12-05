/*******************************************************************************
 * Copyright (c) 2017, 2021 Eurotech and/or its affiliates and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.internal.json.marshaller.unmarshaller;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.inventory.resources.ContainerImage;
import org.eclipse.kura.core.inventory.resources.ContainerImages;
import org.eclipse.kura.core.inventory.resources.DockerContainer;
import org.eclipse.kura.core.inventory.resources.DockerContainers;
import org.eclipse.kura.core.inventory.resources.SystemBundleRef;
import org.eclipse.kura.core.inventory.resources.SystemBundles;
import org.eclipse.kura.core.inventory.resources.SystemDeploymentPackages;
import org.eclipse.kura.core.inventory.resources.SystemPackages;
import org.eclipse.kura.core.inventory.resources.SystemResourcesInfo;
import org.eclipse.kura.core.keystore.util.EntryInfo;
import org.eclipse.kura.internal.json.marshaller.unmarshaller.keystore.KeystoreEntryInfoMapper;
import org.eclipse.kura.internal.json.marshaller.unmarshaller.message.CloudPayloadJsonDecoder;
import org.eclipse.kura.internal.json.marshaller.unmarshaller.message.CloudPayloadJsonEncoder;
import org.eclipse.kura.internal.json.marshaller.unmarshaller.system.JsonJavaContainerImagesMapper;
import org.eclipse.kura.internal.json.marshaller.unmarshaller.system.JsonJavaDockerContainersMapper;
import org.eclipse.kura.internal.json.marshaller.unmarshaller.system.JsonJavaSystemBundleRefMapper;
import org.eclipse.kura.internal.json.marshaller.unmarshaller.system.JsonJavaSystemBundlesMapper;
import org.eclipse.kura.internal.json.marshaller.unmarshaller.system.JsonJavaSystemDeploymentPackagesMapper;
import org.eclipse.kura.internal.json.marshaller.unmarshaller.system.JsonJavaSystemPackagesMapper;
import org.eclipse.kura.internal.json.marshaller.unmarshaller.system.JsonJavaSystemResourcesMapper;
import org.eclipse.kura.internal.json.marshaller.unmarshaller.wiregraph.WireGraphJsonMarshallUnmarshallImpl;
import org.eclipse.kura.marshalling.Marshaller;
import org.eclipse.kura.marshalling.Unmarshaller;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.wire.graph.WireGraphConfiguration;

import com.eclipsesource.json.JsonValue;

public class JsonMarshallUnmarshallImpl implements Marshaller, Unmarshaller {

    @Override
    public String marshal(Object object) throws KuraException {
        final Writer wr = new StringWriter();

        marshal(wr, object);

        return wr.toString();
    }

    @Override
    public void marshal(final OutputStream out, final Object object) throws KuraException {
        marshal(new OutputStreamWriter(out, StandardCharsets.UTF_8), object);
    }

    private void marshal(final Writer wr, final Object object) throws KuraException {

        final JsonValue result;

        if (object instanceof WireGraphConfiguration) {
            result = WireGraphJsonMarshallUnmarshallImpl.marshalWireGraphConfiguration((WireGraphConfiguration) object);
        } else if (object instanceof KuraPayload) {
            result = CloudPayloadJsonEncoder.marshal((KuraPayload) object);
        } else if (object instanceof SystemDeploymentPackages) {
            result = JsonJavaSystemDeploymentPackagesMapper.marshal((SystemDeploymentPackages) object);
        } else if (object instanceof SystemBundles) {
            result = JsonJavaSystemBundlesMapper.marshal((SystemBundles) object);
        } else if (object instanceof SystemPackages) {
            result = JsonJavaSystemPackagesMapper.marshal((SystemPackages) object);
        } else if (object instanceof DockerContainers) {
            result = JsonJavaDockerContainersMapper.marshal((DockerContainers) object);
        } else if (object instanceof ContainerImages) {
            result = JsonJavaContainerImagesMapper.marshal((ContainerImages) object);
        } else if (object instanceof SystemResourcesInfo) {
            result = JsonJavaSystemResourcesMapper.marshal((SystemResourcesInfo) object);
        } else {
            throw new KuraException(KuraErrorCode.INVALID_PARAMETER);
        }

        try {
            result.writeTo(wr);
        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.IO_ERROR, e);
        }
    }

    @Override
    public <T> T unmarshal(String s, Class<T> clazz) throws KuraException {
        return unmarshal(new StringReader(s), clazz);
    }

    @Override
    public <T> T unmarshal(InputStream in, Class<T> clazz) throws KuraException {

        return unmarshal(new InputStreamReader(in, StandardCharsets.UTF_8), clazz);
    }

    @SuppressWarnings("unchecked")
    private <T> T unmarshal(Reader r, Class<T> clazz) throws KuraException {

        try {

            if (clazz.equals(WireGraphConfiguration.class)) {
                return (T) WireGraphJsonMarshallUnmarshallImpl.unmarshalToWireGraphConfiguration(r);
            } else if (clazz.equals(KuraPayload.class)) {
                return (T) CloudPayloadJsonDecoder.buildFromReader(r);
            } else if (EntryInfo.class.isAssignableFrom(clazz)) {
                return (T) KeystoreEntryInfoMapper.unmarshal(r, clazz);
            } else if (clazz.equals(SystemBundleRef.class)) {
                return (T) JsonJavaSystemBundleRefMapper.unmarshal(r);
            } else if (clazz.equals(DockerContainer.class)) {
                return (T) JsonJavaDockerContainersMapper.unmarshal(r);
            } else if (clazz.equals(ContainerImage.class)) {
                return (T) JsonJavaContainerImagesMapper.unmarshal(r);
            }
            throw new IllegalArgumentException("Invalid parameter!");

        } catch (final IOException e) {
            throw new KuraException(KuraErrorCode.IO_ERROR, e);
        }
    }

}
