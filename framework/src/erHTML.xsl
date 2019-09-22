<?xml version="1.0"?> 
<!--
 * Copyright 2014-2020 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
-->

<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:dp="http://www.datapower.com/schemas/management"
    xmlns:dpconfig="http://www.datapower.com/param/config"
    xmlns:exsl="http://exslt.org/common"
    extension-element-prefixes="dp exsl"
    exclude-result-prefixes="dp dpconfig"
>

<xsl:output method="html" encoding="utf-8"/>

<xsl:template name="er_headers">
    <tr>
    <xsl:for-each select="node()">
        <xsl:call-template name="er_header"/>
    </xsl:for-each>
    </tr>
</xsl:template>

<xsl:template name="er_header">
    <xsl:choose>
    <xsl:when test="not(string(.)) and (count(parent::*/preceding-sibling::*)=0)">
        <xsl:choose>
            <xsl:when test="count(../../*)=1">
                <tr>
                    <th class="1"><xsl:value-of select="name(.)"/></th>
                    <td><span class="text"></span></td>
                </tr>
            </xsl:when>
            <xsl:otherwise>
                <th class="1"><xsl:value-of select="name(.)"/></th>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:when>
    <xsl:when test="self::text()">
        <xsl:if test="((string-length(normalize-space(.))>0) and (count(parent::*/parent::*/preceding-sibling::*)=0)) or ((string(name(../..))='EthernetMAUStatus') and  (count(preceding-sibling::*)=0) and (count(parent::*/parent::*/preceding-sibling::*)=0))">
            <xsl:choose>
                <xsl:when test="count(../../../*)=1">
                    <tr>
                        <th class="2">
                            <xsl:value-of select="name(..)"/>
                        </th>
                        <td>
                            <span class="text"><xsl:value-of select="self::text()"/></span>
                        </td>
                    </tr>
                </xsl:when>
                <xsl:otherwise>
                    <th>
                        <xsl:value-of select="name(..)"/>
                    </th>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:if>
    </xsl:when>
    <xsl:when test="self::*">
        <xsl:if test="(count(parent::*/parent::*/parent::*)=0)">
            <xsl:for-each select="node()">
                <xsl:call-template name="er_header"/>
            </xsl:for-each>              
        </xsl:if>
    </xsl:when>
    </xsl:choose>
</xsl:template>

<xsl:template name="flash_table">
    <xsl:param name="indent" select="0"/>
    <xsl:choose>
    <xsl:when test="not(string(.))">
        <td><span class="text"></span></td>
    </xsl:when>
    <xsl:when test="string(.)">
        <td><span class="text"><xsl:value-of select="normalize-space(.)"/></span></td>
        <td><span class="text"><xsl:value-of select="@size"/></span></td>
        <td><span class="text"><xsl:value-of select="@mode"/></span></td>
        <td><span class="text"><xsl:value-of select="@checksum"/></span></td>
    </xsl:when>
    </xsl:choose>
</xsl:template>

<xsl:template name="emau_table">
    <xsl:if test="(count(parent::*/preceding-sibling::*)=0) and  (count(parent::*/parent::*/parent::*/parent::*/preceding-sibling::*)=0)">
        <td>
            <table border="1" class="inner test table">
                <xsl:for-each select="../../node()">
                    <xsl:if test="(string-length(normalize-space(.)) > 0)">
                        <tr>
                            <td style="font-weight:bold"><xsl:value-of select="name(.)"/></td>
                            <td><span class="text"><xsl:value-of select="normalize-space(.)"/></span></td>
                        </tr>
                    </xsl:if>
                </xsl:for-each>
            </table>
        </td>
    </xsl:if>
</xsl:template>

<xsl:template name="table">
    <xsl:param name="indent" select="0"/>
    <xsl:choose>
        <xsl:when test="not(string(.))">
            <td>
                <span class="text"></span>
            </td>
        </xsl:when>
        <xsl:when test="self::text()">
            <xsl:if test="string-length(normalize-space(.)) > 0">
                <xsl:choose>
                    <!-- This is the special case for EthernetMAUStatus -->
                    <xsl:when test="(string(name(../../..))='EthernetMAUStatus')">
                        <xsl:call-template name="emau_table"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <td>
                            <span class="text">
                                <xsl:value-of select="normalize-space(.)"/>
                            </span>
                        </td>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:if>
        </xsl:when>
        <xsl:when test="self::*">
            <xsl:for-each select="node()">
                <xsl:call-template name="table">
                    <xsl:with-param name="indent">
                        <xsl:value-of select="$indent + 1"/>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:for-each>
        </xsl:when>
    </xsl:choose>
</xsl:template>  
  
<xsl:template match="dp:ErrorReport">
    <table border="2">
        <tr>
            <td align="center" colspan="3">
                <xsl:value-of select="@timestamp"/>
            </td>
        </tr>
        <tr>
            <th>Section</th>
            <th>Description</th>
        </tr>
        <xsl:for-each select="dp:Domain/dp:Section">
            <tr>
                <td>
                    <a href="#{@name}"><xsl:value-of select="@name" /></a>
                </td>
                <td>
                    <xsl:value-of select="dp:Description"/>
                </td>
            </tr>                     
        </xsl:for-each>
    </table>
</xsl:template>  

<xsl:template match="Hardware">
    <table border="1">
        <xsl:for-each select="*">
            <tr>
                <th><xsl:value-of select="name()"/></th>
                <td><span class="text"><xsl:value-of select="self::node()"/></span></td>
            </tr>            
        </xsl:for-each>
    </table>
    <br/>
</xsl:template>

<xsl:template match="flash">
    <table border="1">
        <tr>
            <th>File</th>
            <th>Size</th>
            <th>Mode</th>
            <th>Checksum</th>
        </tr>
    <xsl:for-each select="file">
        <tr>
            <xsl:call-template name="flash_table">
                <xsl:with-param name="indent">
                    1
                </xsl:with-param>
            </xsl:call-template>    
        </tr>
    </xsl:for-each>              
    </table>
</xsl:template>
  


<xsl:template match="Status">
    <xsl:choose>
    <xsl:when test="(string-length(normalize-space(.)) > 0)">
    <table border="1">
        <xsl:call-template name="er_headers"/>
            <xsl:if test="count(./*) > 1">
                <xsl:for-each select="node()">          
                <tr>
                    <xsl:call-template name="table">
                        <xsl:with-param name="indent">
                            1
                        </xsl:with-param>
                    </xsl:call-template>
                </tr>
                </xsl:for-each>
            </xsl:if>              
    </table>
    </xsl:when>
    <xsl:otherwise>
         <br></br>
         <span class="text" style="text-size:larger">No Information Available</span>
    </xsl:otherwise>
    </xsl:choose>
    
</xsl:template>

</xsl:stylesheet>
