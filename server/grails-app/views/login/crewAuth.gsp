<html>
<head>
	<meta name='layout' content='taack' />
	<title><g:message code="springSecurity.login.title"/></title>
	<style type='text/css' media='screen'>
		body {
			min-height:600px;
			height: 95%;
		}
		
		#login .inner {
			position: relative;
			width: 340px;
			padding-bottom: 6px;
			margin: 60px auto;
			text-align: left;
			border: 1px solid #aab;
			background-color: #f0f0fa;
			background: white;
			border-radius: 3px;
			box-shadow: 2px 2px 2px #eee, 0px 1px 2px rgba(0, 0, 0, 0.3), 0px 0px 200px rgba(255, 255, 255, .5);
		}
		
		#login:before {
			content: '';
			position: absolute;
			top: -8px;
			right: -8px;
			bottom: -8px;
			left: -8px;
			z-index: -1;
			background: rgba(0, 0, 0, 0.08);
			border-radius: 4px;
			opacity: 0;
		}
		
		#login .inner .fheader {
			margin: -20px -20px 21px;
			line-height: 40px;
			font-size: 15px;
			font-weight: bold;
			color: #555;
			text-align: center;
			text-shadow: 0 1px white;
			background: #f3f3f3;
			border-bottom: 1px solid #cfcfcf;
			border-radius: 3px 3px 0 0;
			linear-gradient: top whiteffd #eef2f5;
			box-shadow: 0 1px #f5f5f5;
			padding: 18px 26px 14px 26px;
			background-color: #f7f7ff;
			margin: 0px 0 14px 0;
			color: #2e3741;
			font-size: 18px;
			font-weight: bold;
		}
		
		.inner p {
			margin: 20px 0 0; 
			margin-left: 20px;
			margin-right: 20px;
		}
		
		.inner p:first-child {
			margin-top: 0;
		}
		
		.inner p input[type=text], .inner p input[type=password] {
			width: 278px;
		}
		
		.inner p#remember_me_holder {
			float: left;
			line-height: 31px;
		}
		
		.inner p#remember_me_holder label {
			font-size: 12px;
			color: #777;
			cursor: pointer;
		}
		
		.inner p#remember_me_holder input {
			position: relative;
			bottom: 1px;
			margin-right: 4px;
			vertical-align: middle;
		}
		
		.inner p#submit {
			text-align: right;
		}
		
		input[type=submit] {
			padding: 0 18px;
			height: 29px;
			font-size: 12px;
			font-weight: bold;
			color: #505E61;
			text-shadow: 0px 1px #e3f1f1;
			background: #cde5ef;
			border: 1px solid;
			border-color: #b4ccce #b3c0c8 #9eb9c2;
			border-radius: 16px;
			outline: 0;
			box-sizing: content-box;
			background: linear-gradient(to bottom, #edf5f8, #cde5ef);
			float: right;
		}
		
		input[type=submit]:active {
			background: #cde5ef;
			border-color: #9eb9c2 #b3c0c8 #b4ccce;
			box-shadow: 0px 0px 1px 1px rgba(0, 0, 0, 0.2) inset;
		}

		br {
			clear: both;
		}
		
		input {
		  font-family: 'Lucida Grande', Tahoma, Verdana, sans-serif;
		  font-size: 14px;
		}
		
		input[type=text], input[type=password] {
		  margin: 5px;
		  padding: 0 10px;
		  width: 200px;
		  height: 34px;
		  color: #404040;
		  background: white;
		  border: 1px solid;
		  border-color: #c4c4c4 #d1d1d1 #d4d4d4;
		  border-radius: 2px;
		  outline: 5px solid #eff4f7;
		  outline-radius: 3px;
		  box-shadow: 0px 1px 3px rgba(0, 0, 0, .12) inset;
		}
		
		input[type=text]:focus, input[type=password]:focus {
		    border-color: #888888;
		    outline-color: #dceefc;
		    outline-offset: 0; // WebKit sets this to -1 by default
		  }

		.login_message {
			color: rgb(145, 0, 0);
			border: 1px solid;
			border-color: #B91E41;
			border-radius: 2px;
			width: 600px;
			min-height: 40px;
			line-height: 40px;
			margin: auto;
			text-align: center;
			border-radius: 15px;
			box-shadow: 1px 1px 1px rgba(255, 255, 255, 0.67) inset;
			background: linear-gradient(#FBD3DA, #F593AA);
		}
	</style>
</head>

<body>
		<g:if test='${flash.message}'>
			<div class='login_message'>${flash.message}</div>
		</g:if>

<div id='login'>
	<div class='inner'>
		<div class='fheader'><g:message code="springSecurity.login.header"/></div>

		<form action='${postUrl}' method='POST' id='loginForm' class='cssform' autocomplete='off'>
			<p>
				<label for='username'><g:message code="springSecurity.login.username.label"/>:</label>
				<input type='text' class='text_' name='username' id='username'/>
			</p>

			<p>
				<label for='password'><g:message code="springSecurity.login.password.label"/>:</label>
				<input type='password' class='text_' name='password' id='password'/>
			</p>

			<p id="remember_me_holder">
<%--        <input type='checkbox' class='chk' name='${rememberMeParameter}' id='remember_me' <g:if test='${hasCookie}'>checked='checked'</g:if>/>--%>
        <input type='checkbox' class='chk' name='remember-me' id='remember_me' checked='checked'/>
				<label for='remember_me'><g:message code="springSecurity.login.remember.me.label"/></label>
			</p>

			<p>
				<input type='submit' id="submit" value='${message(code: "springSecurity.login.button")}'/>
			</p>
			<br />
		</form>
	</div>
</div>
<script type='text/javascript'>
	<!--
	(function() {
		document.forms['loginForm'].elements['j_username'].focus();
	})();
	// -->
</script>
</body>
</html>
