/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
 * by the @authors tag. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
        
package org.mobicents.media.control.mgcp.pkg.au;

/**
 * A return code giving the final status of the operation.
 * <p>
 * Two ranges are defined:<br>
 * <b>100-199:</b> successful completion<br>
 * <b>300-399:</b> error
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public enum ReturnCode {
    
    SUCCESS(100),
    UNSPECIFIED_FAILURE(300),
    BAD_AUDIO_ID(301),
    BAD_SELECTOR_TYPE(302),
    BAD_SELECTOR_VALUE(303),
    VARIABLE_TYPE_NOT_SUPPORTED(304),
    VARIABLE_SUBTYPE_NOT_SUPPORTED(305),
    INVALID_VARIABLE_NAME(306),
    VARIABLE_VALUE_OUT_OF_RANGE(307),
    INCONSISTENT_VARIABLE_SPECIFICATION(308),
    ALIAS_NOT_FOUND(309),
    EXTRA_SEQUENCE_DATA(310),
    MISSING_SEQUENCE_DATA(311),
    MISMATCH_BETWEEN_PLAY_SPECIFICATION_AND_PROVISIONED_DATA(312),
    LANGUAGE_NOT_SET(313),
    REMOVE_OVERRIDE_ERROR(314),
    OVERRIDE_ERROR(315),
    DELETE_AUDIO_ERROR(316),
    UNABLE_TO_RECORD_TEMPORARY_AUDIO(317),
    UNABLE_TO_DELETE_TEMPORARY_AUDIO(318),
    UNABLE_TO_RECORD_PERSISTENT_AUDIO(319),
    UNABLE_TO_DELETE_PERSISTENT_AUDIO(320),
    UNABLE_TO_OVERRIDE_NON_EXISTENT_SEGMENT_ID(321),
    UNABLE_TO_REMOVE_OVERRIDE_FROM_NON_EXISTENT_SEGMENT_ID(322),
    PROVISIONING_ERROR(323),
    UNSPECIFIED_HARDWARE_FAILURE(324),
    SYNTAX_ERROR(325),
    NO_DIGITS(326),
    NO_SPEECH(327),
    SPOKE_TOO_LONG(328),
    DIGIT_PATTERN_NOT_MATCHED(329),
    MAX_ATTEMPTS_EXCEEDED(330);
    
    private final int code;

    private ReturnCode(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
    
}
