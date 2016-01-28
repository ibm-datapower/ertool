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

<xsl:template name="element">
    <xsl:param name="indent" select="0"/>
    <xsl:choose>
      <xsl:when test="self::text()">
		<xsl:if test="string-length(normalize-space(.)) > 0">
              <xsl:value-of select="normalize-space(.)"/>
		</xsl:if>
      </xsl:when>
      <xsl:when test="self::*">
          <xsl:value-of select="concat(name(.),',')"/>
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


  <xsl:template name="table">
    <xsl:choose>
      <xsl:when test="self::text()">
		<xsl:if test="string-length(normalize-space(.)) > 0">
            <xsl:value-of select="concat(normalize-space(.),',')"/>
		</xsl:if>
      </xsl:when>
      <xsl:when test="self::*">
        <xsl:for-each select="node()">
            <xsl:call-template name="table">
            </xsl:call-template>
        </xsl:for-each>              
      </xsl:when>
    </xsl:choose>      
  </xsl:template>


  <xsl:template name="er_header">
    <xsl:choose>
      <xsl:when test="self::text()">
        <xsl:if test="(string-length(normalize-space(.))>0) and (count(parent::*/parent::*/preceding-sibling::*)=0)">
          <xsl:value-of select="concat(name(..),',')"/>
        </xsl:if>
      </xsl:when>
      <xsl:when test="self::*">
         <xsl:for-each select="node()">
            <xsl:call-template name="er_header">
            </xsl:call-template>
         </xsl:for-each>              
      </xsl:when>
    </xsl:choose>      
  </xsl:template>

  <xsl:template match="*">
      <xsl:for-each select="node()">
        <xsl:call-template name="element">
          <xsl:with-param name="indent">
            1
          </xsl:with-param>
        </xsl:call-template>
      </xsl:for-each>              
  </xsl:template>


  <xsl:template match="Status">
  <xsl:choose>
    <xsl:when test="count(node()) > 0">
      <xsl:for-each select="node()">
        <xsl:call-template name="er_header">
        </xsl:call-template>
      </xsl:for-each>              
        <xsl:text>&#xa;</xsl:text>
      <xsl:for-each select="node()">
        <xsl:call-template name="table">
        </xsl:call-template>
        <xsl:text>&#xa;</xsl:text>
      </xsl:for-each> 
    </xsl:when>
    <xsl:otherwise>
      <xsl:text>No status to report</xsl:text>
      <xsl:text>&#xa;</xsl:text>
    </xsl:otherwise>
  </xsl:choose>             
  </xsl:template>


  <xsl:template match="Hardware">
      <xsl:for-each select="node()">
        <xsl:call-template name="er_header">
        </xsl:call-template>
      </xsl:for-each>              
        <xsl:text>&#xa;</xsl:text>
      <xsl:for-each select="node()">
        <xsl:call-template name="table">
        </xsl:call-template>
      </xsl:for-each>              
  </xsl:template>
  

</xsl:stylesheet>

