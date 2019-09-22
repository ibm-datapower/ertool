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
    xmlns:dp="http://www.datapower.com/extensions"
    xmlns:dpconfig="http://www.datapower.com/param/config"
    extension-element-prefixes="dp"
    exclude-result-prefixes="dp dpconfig"
>

  <xsl:output method="text" encoding="utf-8"/>

<xsl:template name="spaces">
    <xsl:param name="indent" select="0"/>
    <xsl:text>  </xsl:text>
    <xsl:if test="$indent > 1">
        <xsl:call-template name="spaces">
            <xsl:with-param name="indent">
              <xsl:value-of select="$indent - 1"/>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:if>
  </xsl:template>


<xsl:template name="element">
    <xsl:param name="indent" select="0"/>
    <xsl:choose>
      <xsl:when test="self::text()">
		<xsl:if test="string-length(normalize-space(.)) > 0">
            <xsl:call-template name="spaces">
                <xsl:with-param name="indent">
                  <xsl:value-of select="$indent"/>
                </xsl:with-param>
            </xsl:call-template>
              <xsl:value-of select="normalize-space(.)"/>
		</xsl:if>
      </xsl:when>
      <xsl:when test="self::*">
        <xsl:call-template name="spaces">
            <xsl:with-param name="indent">
              <xsl:value-of select="$indent"/>
            </xsl:with-param>
        </xsl:call-template>
          <xsl:value-of select="concat(name(.),':  ')"/>
          <xsl:value-of select="self::*/@timestamp"/>
          <xsl:value-of select="self::*/@name"/>
          <xsl:value-of select="self::*/@href"/>
        <xsl:for-each select="node()">
          <xsl:call-template name="element">
            <xsl:with-param name="indent">
              <xsl:value-of select="$indent + 1"/>
            </xsl:with-param>
          </xsl:call-template>
			<xsl:if test="4 >= $indent">
	    		<xsl:text>&#xa;</xsl:text>
			</xsl:if>
        </xsl:for-each>              
      </xsl:when>
    </xsl:choose>      
  </xsl:template>

 
<xsl:template match="*">
  <xsl:choose>
    <xsl:when test="count(node()) > 0">
      <xsl:for-each select="node()">
        <xsl:call-template name="element">
          <xsl:with-param name="indent">
            1
          </xsl:with-param>
        </xsl:call-template>
      </xsl:for-each>               
    </xsl:when>
    <xsl:otherwise>
      <xsl:call-template name="spaces">
        <xsl:with-param name="indent">
          1
        </xsl:with-param>
      </xsl:call-template>
      <xsl:text>No status to report</xsl:text>
    </xsl:otherwise>
  </xsl:choose>            
</xsl:template>


</xsl:stylesheet>

