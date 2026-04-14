// SPDX-FileCopyrightText: © 2025 DSLab - Fondazione Bruno Kessler
//
// SPDX-License-Identifier: Apache-2.0

package it.smartcommunitylab.dhub.rm.model.dto;

import java.util.Map;

import it.smartcommunitylab.dhub.rm.SystemKeys;
import jakarta.validation.constraints.Pattern;

public class SecretDTO {
    
    @Pattern(regexp = SystemKeys.REGEX_CR_ID) 
    private String name;
    private Map<String, String> data;
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Map<String, String> getData() {
        return data;
    }
    public void setData(Map<String, String> data) {
        this.data = data;
    }

    
}
