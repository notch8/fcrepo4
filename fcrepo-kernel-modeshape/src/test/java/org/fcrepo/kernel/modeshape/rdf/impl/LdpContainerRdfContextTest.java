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

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

import org.fcrepo.kernel.modeshape.FedoraResourceImpl;
import org.fcrepo.kernel.modeshape.testutilities.TestNodeIterator;
import org.fcrepo.kernel.modeshape.testutilities.TestPropertyIterator;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.modeshape.jcr.api.NamespaceRegistry;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.Workspace;

import static java.util.stream.Stream.of;
import static org.fcrepo.kernel.api.FedoraTypes.LDP_BASIC_CONTAINER;
import static org.fcrepo.kernel.api.FedoraTypes.LDP_DIRECT_CONTAINER;
import static org.fcrepo.kernel.api.FedoraTypes.LDP_HAS_MEMBER_RELATION;
import static org.fcrepo.kernel.api.FedoraTypes.LDP_INDIRECT_CONTAINER;
import static org.fcrepo.kernel.api.FedoraTypes.LDP_INSERTED_CONTENT_RELATION;
import static org.fcrepo.kernel.api.FedoraTypes.LDP_MEMBER_RESOURCE;
import static org.fcrepo.kernel.api.RdfCollectors.toModel;
import static org.fcrepo.kernel.modeshape.identifiers.NodeResourceConverter.nodeConverter;
import static org.fcrepo.kernel.modeshape.utils.TestHelpers.mockResource;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author cabeer
 */
public class LdpContainerRdfContextTest {

    @Mock
    private FedoraResourceImpl mockResource;

    @Mock
    private Node mockContainerNode;

    @Mock
    private Node mockChild;

    @Mock
    private Node mockNode;

    @Mock
    private PropertyIterator mockReferences;

    @Mock
    private Property mockProperty;

    @Mock
    private Property mockInsertedContentRelationProperty;

    @Mock
    private Property mockRelationProperty;

    @Mock
    private Value mockRelationValue;

    @Mock
    private Session mockSession;

    @Mock
    private Workspace mockWorkspace;

    @Mock
    private NamespaceRegistry mockNamespaceRegistry;

    private DefaultIdentifierTranslator subjects;

    private LdpContainerRdfContext testObj;


    @Before
    public void setUp() throws RepositoryException {
        initMocks(this);
        when(mockNode.getSession()).thenReturn(mockSession);
        mockResource(mockResource, "/a");
        when(mockResource.getNode()).thenReturn(mockNode);
        when(mockSession.getWorkspace()).thenReturn(mockWorkspace);
        when(mockWorkspace.getNamespaceRegistry()).thenReturn(mockNamespaceRegistry);
        subjects = new DefaultIdentifierTranslator(mockSession);
    }

    @Test
    public void testLdpResource() throws RepositoryException {
        when(mockReferences.hasNext()).thenReturn(false);

        when(mockNode.getReferences(LDP_MEMBER_RESOURCE)).thenReturn(mockReferences);
        testObj = new LdpContainerRdfContext(mockResource, subjects);

        final Model model = testObj.collect(toModel());

        assertTrue("Expected stream to be empty", model.isEmpty());
    }

    @Test
    public void testLdpResourceWithEmptyContainer() throws RepositoryException {

        when(mockNode.getReferences(LDP_MEMBER_RESOURCE)).thenReturn(new TestPropertyIterator(mockProperty));
        when(mockProperty.getParent()).thenReturn(mockContainerNode);
        when(mockContainerNode.getNodes()).thenReturn(new TestNodeIterator());
        testObj = new LdpContainerRdfContext(mockResource, subjects);

        final Model model = testObj.collect(toModel());

        assertTrue("Expected stream to be empty", model.isEmpty());

    }


    @Test
    public void testLdpResourceWithBasicContainer() throws RepositoryException {
        when(mockResource.hasType(LDP_BASIC_CONTAINER)).thenReturn(true);
        when(mockNode.getReferences(LDP_MEMBER_RESOURCE)).thenReturn(new TestPropertyIterator());
        when(mockResource.getChildren()).thenReturn(of(mockResource));
        when(mockChild.getName()).thenReturn("b");
        when(mockChild.getPath()).thenReturn("/b");
        testObj = new LdpContainerRdfContext(mockResource, subjects);

        final Model model = testObj.collect(toModel());

        assertTrue("Expected stream to be empty", model.isEmpty());
    }


    @Test
    public void testLdpResourceWithDirectContainerWithoutRelation() throws RepositoryException {
        when(mockContainerNode.isNodeType(LDP_DIRECT_CONTAINER)).thenReturn(true);
        when(mockNode.getReferences(LDP_MEMBER_RESOURCE)).thenReturn(new TestPropertyIterator(mockProperty));
        when(mockProperty.getParent()).thenReturn(mockContainerNode);
        when(mockContainerNode.getNodes()).thenReturn(new TestNodeIterator(mockChild));
        when(mockChild.getName()).thenReturn("b");
        when(mockChild.getPath()).thenReturn("/b");
        testObj = new LdpContainerRdfContext(mockResource, subjects);

        final Model model = testObj.collect(toModel());
        assertTrue("Expected stream to be empty", model.isEmpty());
    }


    @Ignore("until we have a mocking strategy for MODE queries")
    public void testLdpResourceWithDirectContainerAssertingRelation() throws RepositoryException {

        when(mockNode.getReferences(LDP_MEMBER_RESOURCE)).thenReturn(new TestPropertyIterator(mockProperty));
        when(mockProperty.getParent()).thenReturn(mockContainerNode);
        when(mockContainerNode.isNodeType(LDP_DIRECT_CONTAINER)).thenReturn(true);
        when(mockContainerNode.getNodes()).thenReturn(new TestNodeIterator(mockChild));
        when(mockChild.getPath()).thenReturn("/b");
        when(mockChild.getName()).thenReturn("b");
        final Property mockRelation = mock(Property.class);
        when(mockRelation.getString()).thenReturn("some:property");
        when(mockContainerNode.hasProperty(LDP_HAS_MEMBER_RELATION)).thenReturn(true);
        when(mockContainerNode.getProperty(LDP_HAS_MEMBER_RELATION)).thenReturn(mockRelation);
        testObj = new LdpContainerRdfContext(mockResource, subjects);

        final Model model = testObj.collect(toModel());

        assertTrue("Expected stream to have one triple", model.size() == 1);
        assertTrue(model.contains(
                mockResource.graphResource(subjects),
                ResourceFactory.createProperty("some:property"),
                nodeConverter.apply(mockChild).graphResource(subjects)));
    }

    @Ignore("Until we sort out a mocking strategy for ReferencePropertyIterator")
    public void testLdpResourceWithIndirectContainerAssertingRelation() throws RepositoryException {

        when(mockNode.getReferences(LDP_MEMBER_RESOURCE)).thenReturn(new TestPropertyIterator(mockProperty));
        when(mockProperty.getParent()).thenReturn(mockContainerNode);
        when(mockContainerNode.getSession()).thenReturn(mockSession);
        when(mockContainerNode.isNodeType(LDP_INDIRECT_CONTAINER)).thenReturn(true);
        when(mockContainerNode.hasProperty(LDP_INSERTED_CONTENT_RELATION)).thenReturn(true);
        when(mockContainerNode.getProperty(LDP_INSERTED_CONTENT_RELATION)).thenReturn
                (mockInsertedContentRelationProperty);
        when(mockInsertedContentRelationProperty.getString()).thenReturn("some:relation");
        when(mockNamespaceRegistry.isRegisteredUri("some:")).thenReturn(true);
        when(mockNamespaceRegistry.getPrefix("some:")).thenReturn("some");
        when(mockContainerNode.getNodes()).thenReturn(new TestNodeIterator(mockChild));
        when(mockChild.getPath()).thenReturn("/b");
        when(mockChild.getName()).thenReturn("b");
        when(mockChild.hasProperty("some:relation")).thenReturn(true);
        when(mockChild.getProperty("some:relation")).thenReturn(mockRelationProperty);
        when(mockRelationProperty.isMultiple()).thenReturn(false);
        when(mockRelationProperty.getValue()).thenReturn(mockRelationValue);
        when(mockRelationValue.getString()).thenReturn("x");
        final Property mockRelation = mock(Property.class);
        when(mockRelation.getString()).thenReturn("some:property");
        when(mockContainerNode.hasProperty(LDP_HAS_MEMBER_RELATION)).thenReturn(true);
        when(mockContainerNode.getProperty(LDP_HAS_MEMBER_RELATION)).thenReturn(mockRelation);
        testObj = new LdpContainerRdfContext(mockResource, subjects);

        final Model model = testObj.collect(toModel());

        assertTrue("Expected stream to have one triple", model.size() == 1);
        assertTrue(model.contains(
                mockResource.graphResource(subjects),
                ResourceFactory.createProperty("some:property"),
                ResourceFactory.createPlainLiteral("x")));
    }

    @Ignore("Until we sort out a mocking strategy for ReferencePropertyIterator")
    public void testLdpResourceWithIndirectContainerAssertingRelationReference() throws RepositoryException {

        when(mockNode.getReferences(LDP_MEMBER_RESOURCE)).thenReturn(new TestPropertyIterator(mockProperty));
        when(mockProperty.getParent()).thenReturn(mockContainerNode);
        when(mockContainerNode.getSession()).thenReturn(mockSession);
        when(mockContainerNode.isNodeType(LDP_INDIRECT_CONTAINER)).thenReturn(true);
        when(mockContainerNode.hasProperty(LDP_INSERTED_CONTENT_RELATION)).thenReturn(true);
        when(mockContainerNode.getProperty(LDP_INSERTED_CONTENT_RELATION)).thenReturn
                (mockInsertedContentRelationProperty);
        when(mockInsertedContentRelationProperty.getString()).thenReturn("some:relation");
        when(mockNamespaceRegistry.isRegisteredUri("some:")).thenReturn(true);
        when(mockNamespaceRegistry.getPrefix("some:")).thenReturn("some");
        when(mockContainerNode.getNodes()).thenReturn(new TestNodeIterator(mockChild));
        when(mockChild.getPath()).thenReturn("/b");
        when(mockChild.getName()).thenReturn("b");
        when(mockChild.hasProperty("some:relation")).thenReturn(false);
        when(mockChild.hasProperty("some:relation_ref")).thenReturn(true);
        when(mockChild.getProperty("some:relation_ref")).thenReturn(mockRelationProperty);
        when(mockRelationProperty.isMultiple()).thenReturn(false);
        when(mockRelationProperty.getValue()).thenReturn(mockRelationValue);
        when(mockRelationValue.getString()).thenReturn("x");
        final Property mockRelation = mock(Property.class);
        when(mockRelation.getString()).thenReturn("some:property");
        when(mockContainerNode.hasProperty(LDP_HAS_MEMBER_RELATION)).thenReturn(true);
        when(mockContainerNode.getProperty(LDP_HAS_MEMBER_RELATION)).thenReturn(mockRelation);
        testObj = new LdpContainerRdfContext(mockResource, subjects);

        final Model model = testObj.collect(toModel());

        assertTrue("Expected stream to have one triple", model.size() == 1);
        assertTrue(model.contains(
                mockResource.graphResource(subjects),
                ResourceFactory.createProperty("some:property"),
                ResourceFactory.createPlainLiteral("x")));
    }

    @Ignore("until we have a mocking strategy for MODE queries")
    public void testLdpResourceWithIndirectContainerWithoutRelation() throws RepositoryException {

        when(mockNode.getReferences(LDP_MEMBER_RESOURCE)).thenReturn(new TestPropertyIterator(mockProperty));
        when(mockProperty.getParent()).thenReturn(mockContainerNode);
        when(mockContainerNode.getSession()).thenReturn(mockSession);
        when(mockContainerNode.getPath()).thenReturn("/");
        when(mockContainerNode.isNodeType(LDP_INDIRECT_CONTAINER)).thenReturn(true);
        when(mockContainerNode.hasProperty(LDP_INSERTED_CONTENT_RELATION)).thenReturn(true);
        when(mockContainerNode.getProperty(LDP_INSERTED_CONTENT_RELATION)).thenReturn
                (mockInsertedContentRelationProperty);
        when(mockInsertedContentRelationProperty.getString()).thenReturn("some:relation");
        when(mockNamespaceRegistry.isRegisteredUri("some:")).thenReturn(true);
        when(mockNamespaceRegistry.getPrefix("some:")).thenReturn("some");
        when(mockContainerNode.getNodes()).thenReturn(new TestNodeIterator(mockChild));
        when(mockChild.getPath()).thenReturn("/b");
        when(mockChild.getName()).thenReturn("b");
        when(mockChild.hasProperty("some:relation")).thenReturn(false);
        final Property mockRelation = mock(Property.class);
        when(mockRelation.getString()).thenReturn("some:property");
        when(mockContainerNode.hasProperty(LDP_HAS_MEMBER_RELATION)).thenReturn(true);
        when(mockContainerNode.getProperty(LDP_HAS_MEMBER_RELATION)).thenReturn(mockRelation);
        testObj = new LdpContainerRdfContext(mockResource, subjects);

        final Model model = testObj.collect(toModel());
        assertTrue("Expected stream to be empty", model.isEmpty());
    }


    @Test
    public void testLdpResourceWithIndirectContainerWithoutInsertedContentRelation() throws RepositoryException {

        when(mockNode.getReferences(LDP_MEMBER_RESOURCE)).thenReturn(new TestPropertyIterator(mockProperty));
        when(mockProperty.getParent()).thenReturn(mockContainerNode);
        when(mockContainerNode.getSession()).thenReturn(mockSession);
        when(mockContainerNode.isNodeType(LDP_INDIRECT_CONTAINER)).thenReturn(true);
        when(mockContainerNode.hasProperty(LDP_INSERTED_CONTENT_RELATION)).thenReturn(false);
        when(mockContainerNode.getNodes()).thenReturn(new TestNodeIterator(mockChild));
        final Property mockRelation = mock(Property.class);
        when(mockRelation.getString()).thenReturn("some:property");
        when(mockContainerNode.hasProperty(LDP_HAS_MEMBER_RELATION)).thenReturn(true);
        when(mockContainerNode.getProperty(LDP_HAS_MEMBER_RELATION)).thenReturn(mockRelation);
        testObj = new LdpContainerRdfContext(mockResource, subjects);

        final Model model = testObj.collect(toModel());
        assertTrue("Expected stream to be empty", model.isEmpty());
    }

}
