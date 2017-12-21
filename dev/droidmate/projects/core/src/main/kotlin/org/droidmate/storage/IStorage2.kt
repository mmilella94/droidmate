// DroidMate, an automated execution generator for Android apps.
// Copyright (C) 2012-2016 Konrad Jamrozik
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
// email: jamrozik@st.cs.uni-saarland.de
// web: www.droidmate.org

package org.droidmate.storage

import org.droidmate.exploration.data_aggregators.ApkExplorationOutput2
import java.nio.file.Path

/**
 * See {@link org.droidmate.storage.Storage2}
 */
public interface IStorage2 {
    fun delete(deletionTargetNameSuffix: String)

    fun serializeToFile(obj: ApkExplorationOutput2, file: Path)

    fun getSerializedRuns2(): Collection<Path>

    fun deserialize(serPath: Path): ApkExplorationOutput2

    fun serialize(obj: ApkExplorationOutput2, namePart: String)
}