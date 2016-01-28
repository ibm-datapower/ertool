<?xml version="1.0"?> 
<!--
  Licensed Materials - Property of IBM
  IBM WebSphere DataPower Appliances
  Copyright IBM Corporation 2007. All Rights Reserved.
  US Government Users Restricted Rights - Use, duplication or disclosure
  restricted by GSA ADP Schedule Contract with IBM Corp.
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

