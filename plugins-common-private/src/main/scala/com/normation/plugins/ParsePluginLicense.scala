/*
*************************************************************************************
* Copyright 2017 Normation SAS
*************************************************************************************
*
* This file is part of Rudder.
*
* Rudder is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* In accordance with the terms of section 7 (7. Additional Terms.) of
* the GNU General Public License version 3, the copyright holders add
* the following Additional permissions:
* Notwithstanding to the terms of section 5 (5. Conveying Modified Source
* Versions) and 6 (6. Conveying Non-Source Forms.) of the GNU General
* Public License version 3, when you create a Related Module, this
* Related Module is not considered as a part of the work and may be
* distributed under the license agreement of your choice.
* A "Related Module" means a set of sources files including their
* documentation that, without modification of the Source Code, enables
* supplementary functions or services in addition to those offered by
* the Software.
*
* Rudder is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Rudder.  If not, see <http://www.gnu.org/licenses/>.

*
*************************************************************************************
*/

package com.normation.plugins

import java.io.InputStreamReader

import com.normation.license.{LicenseChecker, LicenseError, LicenseReader, RSAKeyManagement, Version}
import com.normation.rudder.domain.logger.PluginLogger
import org.joda.time.DateTime


/*
 * Default licensed implementation of the plugin
 */



/*
 * An utility method that check if a license file exists and is valid.
 * It then analysis it and return these information in a normalized format.
 */

trait LicensedPluginCheck extends CheckRudderPluginEnable {

  /*
   * implementation must define variable with the following maven properties
   * that will be replaced at build time:
   */
//  val pluginClasspathPubkey = "${plugin-resource-publickey}"
//  val pluginLicensePath     = "${plugin-resource-license}"
//  val pluginDeclaredVersion = "${plugin-declared-version}"
//  val pluginId              = "${plugin-name}"

  def pluginClasspathPubkey: String
  def pluginLicensePath    : String
  def pluginDeclaredVersion: String
  def pluginId             : String

  lazy val maybeInfo = LicenseReader.readAndCheckLicense(pluginLicensePath, pluginClasspathPubkey, pluginDeclaredVersion, pluginId)
  lazy val hasLicense = true


  // log
  maybeInfo.fold( error => PluginLogger.error(error) , ok =>  PluginLogger.warn("License signature is valid.") )

  def isEnabled = enabledStatus == PluginStatus.Enabled


  def enabledStatus: PluginStatus = {
    (for {
      info               <- maybeInfo
      (license, version) = info
      check              <- LicenseChecker.checkLicense(license, DateTime.now, version, pluginId)
    } yield {
      check
    }) match {
      case Right(x) => PluginStatus.Enabled
      case Left (y) => PluginStatus.Disabled(y.msg)
    }
  }

  def licenseInformation: Option[PluginLicenseInfo] = maybeInfo match {
    case Left(_)       => None
    case Right((l, v)) => Some(PluginLicenseInfo(
          licensee   = l.content.licensee.value
        , softwareId = l.content.softwareId.value
        , minVersion = l.content.minVersion.value.toString
        , maxVersion = l.content.maxVersion.value.toString
        , startDate  = l.content.startDate.value
        , endDate    = l.content.endDate.value
      ))
  }
}
