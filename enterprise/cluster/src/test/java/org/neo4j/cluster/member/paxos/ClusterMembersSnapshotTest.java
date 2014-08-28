/**
 * Copyright (c) 2002-2014 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cluster.member.paxos;

import java.net.URI;
import java.util.Objects;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Ignore;
import org.junit.Test;

import org.neo4j.cluster.InstanceId;
import org.neo4j.cluster.member.paxos.PaxosClusterMemberEvents.ClusterMembersSnapshot;
import org.neo4j.kernel.impl.nioneo.store.StoreId;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import static org.neo4j.helpers.collection.IteratorUtil.count;

@Ignore("Ignored temporarily, pending review for extracting useful bits. The bulk of the test is now in HA, HaNewSnapshotFunctionTest")
public class ClusterMembersSnapshotTest
{
    private static final String URI = "http://me";
    private static final String ROLE_1 = "r1";
    private static final String ROLE_2 = "r2";

    @Test
    public void snapshotListPrunesSameMemberOnIdenticalAvailabilityEvents() throws Exception
    {
        // GIVEN
        // -- a snapshot containing one member with a role
        ClusterMembersSnapshot snapshot = new ClusterMembersSnapshot( new PaxosClusterMemberEvents.UniqueRoleFilter() );
        URI clusterUri = new URI( URI );
        InstanceId instanceId = new InstanceId( 1 );
        MemberIsAvailable memberIsAvailable = new MemberIsAvailable(
                ROLE_1, instanceId, clusterUri, new URI( URI + "?something" ), StoreId.DEFAULT );
        snapshot.availableMember( memberIsAvailable );

        // WHEN
        // -- the same member and role gets added to the snapshot
        snapshot.availableMember( memberIsAvailable );

        // THEN
        // -- getting the snapshot list should only reveal the last one
        assertEquals( 1, count( snapshot.getCurrentAvailable( instanceId ) ) );
        assertThat( snapshot.getCurrentAvailable( instanceId ), hasItem( memberIsAvailable( memberIsAvailable ) ) );
        assertEquals( 1, count( snapshot.getCurrentAvailableMembers() ) );
        assertThat( snapshot.getCurrentAvailableMembers(), hasItems( memberIsAvailable( memberIsAvailable ) ) );
    }
    
    @Test
    public void snapshotListCanContainMultipleEventsWithSameMemberWithDifferentRoles() throws Exception
    {
        // GIVEN
        // -- a snapshot containing one member with a role
        ClusterMembersSnapshot snapshot = new ClusterMembersSnapshot(null);
        URI clusterUri = new URI( URI );
        InstanceId instanceId = new InstanceId( 1 );
        MemberIsAvailable event1 = new MemberIsAvailable(
                ROLE_1, instanceId, clusterUri, new URI( URI + "?something" ), StoreId.DEFAULT );
        snapshot.availableMember( event1 );

        // WHEN
        // -- the same member, although different role, gets added to the snapshot
        MemberIsAvailable event2 = new MemberIsAvailable(
                ROLE_2, instanceId, clusterUri, new URI( URI + "?something" ), StoreId.DEFAULT );
        snapshot.availableMember( event2 );

        // THEN
        // -- getting the snapshot list should reveal both
        assertEquals( 2, count( snapshot.getCurrentAvailable( instanceId ) ) );
        assertThat(
                snapshot.getCurrentAvailable( instanceId ),
                hasItems( memberIsAvailable( event1 ), memberIsAvailable( event2 ) ) );
        assertEquals( 2, count( snapshot.getCurrentAvailableMembers() ) );
        assertThat(
                snapshot.getCurrentAvailableMembers(),
                hasItems( memberIsAvailable( event1 ), memberIsAvailable( event2 ) ) );
    }
    
    @Test
    public void snapshotListPrunesOtherMemberWithSameRole() throws Exception
    {
        // GIVEN
        // -- a snapshot containing one member with a role
        ClusterMembersSnapshot snapshot = new ClusterMembersSnapshot(null);
        URI clusterUri = new URI( URI );
        InstanceId instanceId = new InstanceId( 1 );
        MemberIsAvailable event = new MemberIsAvailable(
                ROLE_1, instanceId, clusterUri, new URI( URI + "?something1" ), StoreId.DEFAULT );
        snapshot.availableMember( event );

        // WHEN
        // -- another member, but with same role, gets added to the snapshot
        URI otherClusterUri = new URI( URI );
        InstanceId otherInstanceId = new InstanceId( 2 );
        MemberIsAvailable otherEvent = new MemberIsAvailable(
                ROLE_1, otherInstanceId, otherClusterUri, new URI( URI + "?something2" ), StoreId.DEFAULT );
        snapshot.availableMember( otherEvent );

        // THEN
        // -- getting the snapshot list should only reveal the last member added, as it had the same role
        assertEquals( 1, count( snapshot.getCurrentAvailable( otherInstanceId ) ) );
        assertThat( snapshot.getCurrentAvailable( otherInstanceId ), hasItems( memberIsAvailable( otherEvent ) ) );
        assertEquals( 1, count( snapshot.getCurrentAvailableMembers() ) );
        assertThat( snapshot.getCurrentAvailableMembers(), hasItems( memberIsAvailable( otherEvent ) ) );
    }
    
    private static Matcher<MemberIsAvailable> memberIsAvailable( final MemberIsAvailable expected )
    {
        return new BaseMatcher<MemberIsAvailable>()
        {
            @Override
            public boolean matches( Object item )
            {
                MemberIsAvailable input = (MemberIsAvailable) item;
                return Objects.equals( input.getClusterUri(), expected.getClusterUri() ) &&
                        Objects.equals( input.getRole(), expected.getRole() ) &&
                        Objects.equals( input.getRoleUri(), expected.getRoleUri() );
            }

            @Override
            public void describeTo( Description description )
            {
            }
        };
    }
}
