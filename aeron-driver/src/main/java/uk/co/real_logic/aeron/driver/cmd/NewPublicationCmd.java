/*
 * Copyright 2014 - 2015 Real Logic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.real_logic.aeron.driver.cmd;

import uk.co.real_logic.aeron.driver.DriverPublication;
import uk.co.real_logic.aeron.driver.RetransmitHandler;
import uk.co.real_logic.aeron.driver.Sender;
import uk.co.real_logic.aeron.driver.SenderFlowControl;

public class NewPublicationCmd implements SenderCmd
{
    private final DriverPublication publication;
    private final RetransmitHandler retransmitHandler;
    private final SenderFlowControl senderFlowControl;

    public NewPublicationCmd(
        final DriverPublication publication,
        final RetransmitHandler retransmitHandler,
        final SenderFlowControl senderFlowControl)
    {
        this.publication = publication;
        this.retransmitHandler = retransmitHandler;
        this.senderFlowControl = senderFlowControl;
    }

    public void execute(final Sender sender)
    {
        sender.onNewPublication(publication, retransmitHandler, senderFlowControl);
    }
}
