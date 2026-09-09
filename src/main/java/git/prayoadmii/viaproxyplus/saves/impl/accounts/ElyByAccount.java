/*
 * This file is part of ViaProxy - https://github.com/RaphiMC/ViaProxy
 * Copyright (C) 2021-2026 RK_01/RaphiMC and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package git.prayoadmii.viaproxyplus.saves.impl.accounts;

import com.google.gson.JsonObject;
import git.prayoadmii.viaproxyplus.ViaProxy;

import java.util.UUID;

public class ElyByAccount extends Account {

    private final String name;
    private final UUID uuid;
    private final String accessToken;
    private final String clientToken;

    public ElyByAccount(final JsonObject jsonObject) {
        this.name = jsonObject.get("name").getAsString();
        this.uuid = UUID.fromString(jsonObject.get("uuid").getAsString());
        this.accessToken = jsonObject.get("accessToken").getAsString();
        this.clientToken = jsonObject.has("clientToken") ? jsonObject.get("clientToken").getAsString() : "";
    }

    public ElyByAccount(final String name, final UUID uuid, final String accessToken, final String clientToken) {
        this.name = name;
        this.uuid = uuid;
        this.accessToken = accessToken;
        this.clientToken = clientToken;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("name", this.name);
        json.addProperty("uuid", this.uuid.toString());
        json.addProperty("accessToken", this.accessToken);
        json.addProperty("clientToken", this.clientToken);
        return json;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public UUID getUUID() {
        return this.uuid;
    }

    public String getAccessToken() {
        return this.accessToken;
    }

    public String getClientToken() {
        return this.clientToken;
    }

    @Override
    public String getDisplayString() {
        return this.name + " (Ely.by)";
    }
}
