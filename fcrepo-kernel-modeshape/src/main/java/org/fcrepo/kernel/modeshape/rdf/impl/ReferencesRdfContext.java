/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.kernel.modeshape.rdf.impl;

import static org.fcrepo.kernel.modeshape.identifiers.NodeResourceConverter.nodeConverter;
import static org.fcrepo.kernel.modeshape.rdf.converters.ValueConverter.nodeForValue;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getJcrNode;
import static org.fcrepo.kernel.modeshape.utils.StreamUtils.iteratorToStream;
import static org.fcrepo.kernel.modeshape.utils.UncheckedFunction.uncheck;
import static java.util.Arrays.asList;
import static javax.jcr.PropertyType.PATH;
import static javax.jcr.PropertyType.REFERENCE;
import static javax.jcr.PropertyType.WEAKREFERENCE;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Resource;

import org.fcrepo.kernel.api.functions.Converter;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.modeshape.identifiers.InternalPathToNodeConverter;
import org.fcrepo.kernel.modeshape.rdf.impl.mappings.PropertyToTriple;
import org.fcrepo.kernel.modeshape.rdf.impl.mappings.PropertyValueIterator;
import org.fcrepo.kernel.modeshape.rdf.impl.mappings.ResourceUriValue;
import org.fcrepo.kernel.modeshape.rdf.impl.mappings.RowToResourceUri;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Accumulate inbound references to a given resource
 *
 * @author cabeer
 * @author escowles
 */
public class ReferencesRdfContext extends NodeRdfContext {

    private final PropertyToTriple property2triple;

    public static final List<Integer> REFERENCE_TYPES = asList(PATH, REFERENCE, WEAKREFERENCE);

    /**
     * Add the inbound references from other nodes to this resource to the stream
     *
     * @param resource the resource
     * @param idTranslator the id translator
     * @throws RepositoryException if repository exception occurred
     */

    public ReferencesRdfContext(final FedoraResource resource,
                                final Converter<Resource, String> idTranslator)
        throws RepositoryException {
        super(resource, idTranslator);
        property2triple = new PropertyToTriple(getJcrNode(resource).getSession(), idTranslator);
        concat(putReferencesIntoContext(getJcrNode(resource)));
    }

    private final Predicate<Triple> INBOUND = t -> {
        return t.getObject().getURI().equals(uriFor(resource()).getURI());
    };

    /* References from LDP indirect containers are generated dynamically by LdpContainerRdfContext, so they won't
       show up in getReferences()/getWeakReferences().  Instead, we should check referencers to see if they are
       members of an IndirectContainer and generate the appropriate inbound references. */
    private Stream<Triple> putReferencesIntoContext(final Node node) throws RepositoryException {
        final Converter<Resource, Node> converter =
                translator().andThen(new InternalPathToNodeConverter(node.getSession()));
        return Stream.concat(
            getAllReferences(node).flatMap(property2triple),
            getAllReferences(node).flatMap(uncheck((final Property x) -> {
                    @SuppressWarnings("unchecked")
                    final Stream<Value> values = iteratorToStream(new PropertyValueIterator(x.getParent()
                            .getProperties(), new RowToResourceUri(translator())))
                            .filter((final Value y) -> {
                                return REFERENCE_TYPES.contains(y.getType()) || y instanceof ResourceUriValue;
                            });
                    return values;
                }))
                .flatMap(uncheck((final Value x) -> {
                    if (x instanceof ResourceUriValue) {
                        final Resource referee = ((ResourceUriValue)x).getURI();
                        return new LdpContainerRdfContext(referee, node.getSession(), translator());
                    }
                    return new LdpContainerRdfContext(
                            nodeConverter.apply(nodeForValue(node.getSession(), x, converter)),
                            translator());
                }))
                .filter(INBOUND));
    }

    @SuppressWarnings("unchecked")
    private static Stream<Property> getAllReferences(final Node node) throws RepositoryException {
        return Stream.concat(iteratorToStream(node.getReferences()), iteratorToStream(node.getWeakReferences()));
    }
}
