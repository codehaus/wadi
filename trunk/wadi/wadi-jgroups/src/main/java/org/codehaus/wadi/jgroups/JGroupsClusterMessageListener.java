package org.codehaus.wadi.jgroups;

import org.codehaus.wadi.group.Message;
import org.codehaus.wadi.jgroups.messages.StateRequest;
import org.codehaus.wadi.jgroups.messages.StateUpdate;

public interface JGroupsClusterMessageListener {
    void onMessage(Message message, StateUpdate update) throws Exception;

    void onMessage(Message message, StateRequest request) throws Exception;
}