/* Copyright 2013-2016 the original author or authors.
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
package taack.website

import grails.compiler.GrailsCompileStatic
import grails.config.Config
import grails.core.support.GrailsConfigurationAware
import grails.plugin.springsecurity.SpringSecurityService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.annotation.Secured
import taack.ui.TaackUiConfiguration

@GrailsCompileStatic
@Secured('permitAll')
class LoginController extends grails.plugin.springsecurity.LoginController implements GrailsConfigurationAware {

	@Autowired
	TaackUiConfiguration taackUiPluginConfiguration
	/** Show the login page. */
	def auth() {

		ConfigObject conf = getConf()

		if ((springSecurityService as SpringSecurityService).isLoggedIn()) {
			redirect uri: conf.successHandler["defaultTargetUrl"]
			return
		}

		String postUrl = request.contextPath + conf.apf["filterProcessesUrl"]
		render view: 'crewAuth', model: [postUrl: postUrl,
		                             rememberMeParameter: conf.rememberMe["parameter"],
		                             usernameParameter: conf.apf["usernameParameter"],
		                             passwordParameter: conf.apf["passwordParameter"],
									 conf: taackUiPluginConfiguration,
		                             gspLayout: conf.gsp["layoutAuth"]]
	}

	@Override
	void setConfiguration(Config co) {

	}
}
