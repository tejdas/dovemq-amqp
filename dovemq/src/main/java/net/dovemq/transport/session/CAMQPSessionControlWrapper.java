/**
 * Copyright 2012 Tejeswar Das
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.dovemq.transport.session;

final class CAMQPSessionControlWrapper {
    CAMQPSessionControlWrapper(int channelNumber, Object sessionControl) {
        super();
        this.channelNumber = channelNumber;
        this.sessionControl = sessionControl;
    }

    CAMQPSessionControlWrapper(Object sessionControl) {
        super();
        this.sessionControl = sessionControl;
    }

    int getChannelNumber() {
        return channelNumber;
    }

    void setChannelNumber(int channelNumber) {
        this.channelNumber = channelNumber;
    }

    Object getSessionControl() {
        return sessionControl;
    }

    boolean isControlInitiator() {
        return isControlInitiator;
    }

    void setControlInitiator(boolean isControlInitiator) {
        this.isControlInitiator = isControlInitiator;
    }

    private boolean isControlInitiator = false;

    private int channelNumber = 0;

    private final Object sessionControl;
}
