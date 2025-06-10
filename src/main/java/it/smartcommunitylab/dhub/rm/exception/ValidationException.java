// SPDX-License-Identifier: Apache-2.0
package it.smartcommunitylab.dhub.rm.exception;

import com.networknt.schema.ValidationMessage;
import java.util.Set;
import java.util.stream.Collectors;

public class ValidationException extends RuntimeException {

    public ValidationException(Set<ValidationMessage> errors) {
        super("The following validation errors were found: " + errors.stream().map(ValidationMessage::getMessage).collect(Collectors.joining("; ")));
    }
}
