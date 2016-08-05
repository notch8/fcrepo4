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

import com.hp.hpl.jena.rdf.model.Resource;

import org.fcrepo.kernel.api.exception.RepositoryRuntimeException;
import org.fcrepo.kernel.api.functions.Converter;
import org.fcrepo.kernel.api.models.FedoraResource;
import org.fcrepo.kernel.modeshape.identifiers.InternalPathToNodeConverter;
import org.fcrepo.kernel.modeshape.rdf.impl.mappings.PropertyValueIterator;
import org.fcrepo.kernel.modeshape.rdf.impl.mappings.RowToResourceUri;

import org.slf4j.Logger;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import java.util.Objects;
import java.util.stream.Stream;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.fcrepo.kernel.api.RequiredRdfContext.PROPERTIES;
import static org.fcrepo.kernel.modeshape.identifiers.NodeResourceConverter.nodeConverter;
import static org.fcrepo.kernel.modeshape.rdf.converters.ValueConverter.nodeForValue;
import static org.fcrepo.kernel.modeshape.rdf.impl.ReferencesRdfContext.REFERENCE_TYPES;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.getJcrNode;
import static org.fcrepo.kernel.modeshape.utils.FedoraTypesUtils.isSkolemNode;
import static org.fcrepo.kernel.modeshape.utils.UncheckedPredicate.uncheck;
import static org.fcrepo.kernel.modeshape.utils.StreamUtils.iteratorToStream;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Embed triples describing all skolem nodes in the RDF stream
 *
 * @author cabeer
 * @author ajs6f
 * @since 10/9/14
 */
public class SkolemNodeRdfContext extends NodeRdfContext {

    private static final Logger LOGGER = getLogger(SkolemNodeRdfContext.class);

    /**
     * Default constructor.
     *
     * @param resource the resource
     * @param idTranslator the idTranslator
     * @throws RepositoryException if a repository exception occurred
     */
    public SkolemNodeRdfContext(final FedoraResource resource,
                               final Converter<Resource, String> idTranslator)
            throws RepositoryException {
        super(resource, idTranslator);

        concat(getBlankNodes(resource, idTranslator).flatMap(n -> nodeConverter.apply(n).getTriples(idTranslator,
                    PROPERTIES)));
    }

    @SuppressWarnings("unchecked")
    private static Stream<Node> getBlankNodes(final FedoraResource resource,
            final Converter<Resource, String> idTranslator)
            throws RepositoryException {
        final Session session = getJcrNode(resource).getSession();
        final Converter<Resource, Node> converter =
                idTranslator.andThen(new InternalPathToNodeConverter(session));
        final Function<Value, Node> valueToNode = sessionValueToNode.apply(session, converter);
        final Stream<Property> refs = iteratorToStream(getJcrNode(resource).getProperties())
                .filter(uncheck((final Property p) -> REFERENCE_TYPES.contains(p.getType())));
        return iteratorToStream(new PropertyValueIterator(refs.iterator(), new RowToResourceUri(idTranslator)))
                .map(valueToNode)
                .filter(Objects::nonNull)
                .filter(isSkolemNode);
    }

    private static final BiFunction<Session, Converter<Resource, Node>, Function<Value, Node>> sessionValueToNode =
        (session, idTranslator) -> v -> {
        try {
            return nodeForValue(session, v, idTranslator);

        } catch (final AccessDeniedException e) {
            LOGGER.error("Link inaccessible by requesting user: {}, {}", v, session.getUserID());
            return null;

        } catch (final RepositoryException e) {
            throw new RepositoryRuntimeException(e);
        }
    };
}
