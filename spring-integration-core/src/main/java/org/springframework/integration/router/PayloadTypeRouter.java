/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.router;

import java.util.Collections;
import java.util.List;

import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageHandlingException;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * A Message Router that resolves the {@link MessageChannel} based on the
 * {@link Message Message's} payload type.
 * 
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class PayloadTypeRouter extends AbstractMessageRouter {

	/**
	 * Selects the most appropriate channel name matching channel identifiers which are the
	 * fully qualified class names encountered while traversing the payload type hierarchy.
	 * To resolve ties and conflicts (e.g., Serializable and String) it will match:
	 * 1. Type name to channel identifier else...
	 * 2. Name of the subclass of the type to channel identifier else...
	 * 3. Name of the Interface of the type to channel identifier while also
	 *    preferring direct interface over indirect subclass
	 */
	@Override
	protected List<Object> getChannelIdentifiers(Message<?> message) {
		Class<?> firstInterfaceMatch = null;
		Class<?> type = message.getPayload().getClass();
		while (type != null && !CollectionUtils.isEmpty(this.channelIdentifierMap)) {
			Class<?>[] interfaces = type.getInterfaces();
			// first try to find a match amongst the interfaces and also check if there is more than one
			for (Class<?> ifc : interfaces) {
				if (this.channelIdentifierMap.containsKey(ifc.getName())) {
					if (firstInterfaceMatch != null) {
						throw new MessageHandlingException(message,
								"Unresolvable ambiguity while attempting to find closest match for [" + type.getName() +
								"]. Candidate types [" + firstInterfaceMatch.getName() + "] and [" +
								ifc.getName() + "] have equal weight.");
					}
					else {
						firstInterfaceMatch = ifc;
					}
				}
			}
			// the actual type should favor the possible interface match
			String channelName = this.channelIdentifierMap.get(type.getName());
			if (!StringUtils.hasText(channelName)) {
				if (firstInterfaceMatch != null) {
					return Collections.singletonList((Object) this.channelIdentifierMap.get(firstInterfaceMatch.getName()));
				}	
			}
			else {
				return Collections.singletonList((Object)channelName);
			}
			type = type.getSuperclass();
		}
		return null;
	}

}
