/*
 * Copyright (c) 2016-2018 Flux Capacitor.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fluxcapacitor.javaclient.tracking.handling.validation;

import lombok.AllArgsConstructor;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import java.util.Set;

@AllArgsConstructor
public class Jsr380Validator implements Validator {
    private final javax.validation.Validator validator;

    public static Jsr380Validator createDefault() {
        return new Jsr380Validator(Validation.buildDefaultValidatorFactory().getValidator());
    }

    @Override
    public <T> T validate(T object) throws ValidationException {
        Set<? extends ConstraintViolation<?>> violations = validator.validate(object);
        if (!violations.isEmpty()) {
            throw new ValidationException(violations);
        }
        return object;
    }
}