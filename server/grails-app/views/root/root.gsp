<%@ page import="taack.website.RootController" %>
<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="taack"/>
    <title>Taack - Intranet</title>
    <asset:stylesheet href="grids-responsive-min.css"/>
    <asset:stylesheet href="taack.css"/>
    <style type="text/css">
    /*a:not(.user-info) {*/
    /*    color: #425B6A !important;*/
    /*}*/

    h4 {
        margin: auto;
    }

    .no-access {
        opacity: 0.3;
    }

    .taackContainer img {
        max-height: 130px;
        max-width: 130px;
        width: 100%;
        height: 100%;
    }

    .oldContainer {
        zoom: 80%;
        text-align: center;
    }
    .oldContainer img {
        /*max-width: 50%;*/
        height: 180px;
        margin: 16px;
    }
    </style>
    <asset:stylesheet src="my.css"/>
</head>

<body>
<div class="pure_g" style="text-align: center; margin: 30px 0;">
    <g:each in="${taackPluginService.taackPlugins*.taackPluginControllerConfigurations.flatten().reverse()}">
        <%
            def pluginConfiguration = it
            String pluginRoles = (pluginConfiguration.pluginRole.pluginRoles*.roleName + "ROLE_ADMIN").join(',')
        %>
        <g:if test="${pluginConfiguration.name != 'Base' && !pluginConfiguration.hideIcon}">
            <div class="pure-u-1-2 pure-u-sm-1-3 pure-u-md-1-4 pure-u-lg-1-5 pure-u-xl-1-8 taackContainer"
                 style="text-align: center;">
                <sec:access controller="${pluginConfiguration.mainControllerName}">
                    <g:link controller="${pluginConfiguration.mainControllerName}">
                        <img src="/root/getPluginLogo?pluginControllerName=${pluginConfiguration.mainControllerName}&randIcon=${RootController.randIcon}"/>

                        <div style="vertical-align: middle">
                            <div>${pluginConfiguration.name}</div>
                        </div>
                    </g:link>
                </sec:access>
                <sec:noAccess controller="${pluginConfiguration.mainControllerName}">
                    <img src="/root/getPluginLogo?pluginControllerName=${pluginConfiguration.mainControllerName}"
                         style="width: 60%" class="no-access"/>

                    <div class="no-access" style="vertical-align: middle">
                        <div>${pluginConfiguration.name}</div>
                    </div>

                    <div style="vertical-align: middle;">
                        <span style="font-size: xx-small;">${pluginRoles.replace(',', ' ')}</span>
                    </div>
                </sec:noAccess>
            </div>
        </g:if>
    </g:each>
</div>
</body>
</html>
