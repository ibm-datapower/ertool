#!/bin/bash

SHRED="auto"
TMP_FILE=/tmp/bso_auth.$$
cleanup() {
   [ -e "${TMP_FILE}" ] && del "${TMP_FILE}"; return 0
}

trap cleanup INT 

DL_CLIENT=auto
USE_HTTP=1
USE_HTTPS=1
USED_METHOD=""
DL_CLIENTS=${DL_CLIENTS:-"curl wget"}
BSO_HEADER_STRING="BSO Firewall|Authentication Proxy Login Page"
PING_CHECK=1

host=$(hostname -f)
check_file="__bso_auth_check__?host=${host}"

rhost=${1}
check_url="http://${rhost}/${check_file}"

# you can put 
#  AUTH_USER="you@us.ibm.com"
#  AUTH_PASS="your_pass"
#  in SITE_CONF or USER_CONF and not have to type passwords
SITE_CONF=/etc/bso_auth.conf
USER_CONF=$HOME/bso_auth.conf



Usage() {
   cat <<EOF
Usage: ${0##*/} host
   auth to the host
EOF
}

do_exit() {
   ret=${1}; shift; cleanup
   [ $# -ne 0 ] && echo "${@}"
   exit ${ret}
}

del() {
   [ -z "${SHRED}" -o "${SHRED}" = "auto" ] && 
      { which shred &>/dev/null && SHRED=shred || SHRED="rm" ; }
   local f=""
   for f in "$@"; do
      [ -e "$f" ] || continue
      if [ "${SHRED}" = "shred" ]; then
         shred -u -f "${@}" || { rm -f "${@}"; return 1 ; }
      else
         rm -f "${@}" || return 1
      fi
   done
   return 0
}

[ $# -eq 0 -o "${1}" = "-h" -o "${1}" = "--help" ] && { Usage ; do_exit 1; }

get_url() {
   local url="$1" out="$2" headers="$3"
   MAX_TIME=${MAX_TIME:-10}
   local curl_headers="" ; [ -n "$3" ] && curl_headers="--dump-header -"
   local wget_headers="" ; [ -n "$3" ] && wget_headers="-S --save-headers"
   case "${DL_CLIENT}" in
      *wget) #cmd="wget --no-check-certificate --tries 1 --timeout ${MAX_TIME}"
             cmd="wget --no-check-certificate --tries 1" 
             cmd="${cmd} ${wget_headers} '${url}' -O -";;
      *curl) #cmd="curl --connect-timeout 5 -s --retry 0 --insecure --max-time ${MAX_TIME} "
             cmd="curl --connect-timeout 5 -s --retry 0 --insecure"
             cmd="${cmd} --connect-timeout 5 ${curl_headers} -o - '${url}'";;
      *) do_exit 1 "don't know $DL_CLIENT";;
   esac
   eval "${cmd}" > ${out} 2>&1
   return
}

is_authed() {

	url=${1}
   get_url "${url}" "${TMP_FILE}" 1
   egrep "${BSO_HEADER_STRING}" "${TMP_FILE}" &>/dev/null && return 1
   return 0
}

get_user_pass() {

	local fp=${1}
	[ "${fp}" = "1" ] && fp="";
	[ -e "${SITE_CONF}" ] && source $SITE_CONF
	[ -e "${USER_CONF}" ] && source $USER_CONF


	[ -z "${AUTH_USER}" ] &&
		{ AUTH_USER="username@ibm.com"; }
	[ -z "${AUTH_PASS}" ] &&
		{ AUTH_PASS="intranetpassword"; }

#	[ -z "${AUTH_USER}" -o ! -z "${fp}" ] &&
#		{ read -p "w3_username:" AUTH_USER || return 1; }
#	[ -z "${AUTH_PASS}" -o ! -z "${fp}" ] &&
#		{ read -s -p "w3_pass:" AUTH_PASS && echo || return 1; }
	return 0
}

empty_tmp() {
   { [ ! -e "${1}" ] || del "${1}"; } && touch "${1}" && chmod 600 "${1}"
}

wget_auth() {
   local url=${1} user=${2} pass=${3}
   local wgetrc=$HOME/.wgetrc
   local saved_wgetrc=""
   empty_tmp "${TMP_FILE}" && { 
         echo -E "http_user = ${user}" &&
         echo -E "http_passwd = ${pass}"
      } >> "${TMP_FILE}" || do_exit 1 "failed to write wgetrc";

   [ -e "${wgetrc}" ] && mv "$wgetrc" "$wgetrc.tmp" && 
      saved_wgetrc="${wgetrc}.tmp" 

   ln -s "${TMP_FILE}" "${wgetrc}" ||
      do_exit 1 "failed to link ${TMP_FILE} to ${wgetrc}"

   wget ${url} -O - &>/dev/null
   rm -f "${wgetrc}"  &&
      del "${TMP_FILE}" || 
      do_exit 1 "delete of ${TMP_FILE} or ${wgetrc} failed"
   if [ ! -z "${saved_wgetrc}" ]; then
      mv "${saved_wgetrc}" "${wgetrc}" || 
      do_exit 1 "failed to restore ${wgetrc} from ${saved_wgetrc}";
   fi
   return 0
}

curl_auth() {
   # would like to attempt to write a curl config and pass -K config
   # then clear it
   local url=${1}
   local user=${2}
   local pass=${3}
   empty_tmp "${TMP_FILE}" &&
      echo -E "user = \"${user}:${pass}\"" >> "${TMP_FILE}" ||
      do_exit 1 "failed to write ${TMP_FILE} curl config";
   curl -K "${TMP_FILE}" "${url}" &>/dev/null
   del "${TMP_FILE}" || 
      do_exit 1 "delete ${TMP_FILE} failed. has sensitive data";
   return 0
}

do_auth_https() {
   local url="${1}" user="${2}" pass="${3}"
   local timetag="" tfield="au_pxytimetag" cmd="" ret=""
   # the order of the parameters is important. the only thing that works
   # is the order uname, pwd, 
   local data="uname=${user}&pwd=${pass}&ok=OK"
   # first have to do an https get and grab the au_pxytimetag field
   url="https://${url#*://}";
   get_url "${url}" "${TMP_FILE}"
   timetag=$(sed -n -e \
      '/au_pxytimetag/s,.*name=au_pxytimetag value=["]*\([^">]*\).*,\1,p' \
      < ${TMP_FILE}) || return 1
   data="${tfield}=${timetag}&${data}"
   empty_tmp "${TMP_FILE}" && echo -E "$data" > "${TMP_FILE}" ||
      do_exit 1 "failed to write data to ${TMP_FILE}"
   case "${DL_CLIENT}" in
      *wget) wget -q --no-check-certificate --tries 1 --timeout 10 \
         "--post-file=${TMP_FILE}" "${url}" -O - 2>&1 | 
         grep -q "BSO Authentication Successful" ;;
      *curl) curl --insecure --connect-timeout 5 -s --retry 0 --data "@${TMP_FILE}" -o - \
         "${url}" 2>&1 | grep -q "BSO Authentication Successful" ;;
      *) do_exit 1 "unknown client ${DL_CLIENT}"
   esac
   ret=$?
   [ $ret -eq 0 ] && USED_METHOD="https"
   del "${TMP_FILE}"
   return $ret
}

do_auth() {
	local url=${1} user=${2} pass=${3}
   if [ -n "${USE_HTTPS}" -a "${USE_HTTPS}" != "0" ]; then
      do_auth_https "${url}" "${user}" "${pass}" && return
   fi
   if [ -n "${USE_HTTP}" -a "${USE_HTTP}" != "0" ]; then
      USED_METHOD="http"
      case "${DL_CLIENT}" in
         *wget) wget_auth "${url}" "${user}" "${pass}" && return ;;
         *curl) curl_auth "${url}" "${user}" "${pass}" && return ;;
         *) do_exit 1 "don't know $DL_CLIENT";;
      esac
   fi
}

if [ ! -z "$PING_CHECK" -a "$PING_CHECK" != "0" ]; then
   ping -c 1 "${rhost}" &>/dev/null || 
      do_exit 1 "failed to ping check ${rhost}. does this host exist?"
fi

if [ -z "$DL_CLIENT" -o "$DL_CLIENT" = "auto" ]; then
   success=0
	for client in $DL_CLIENTS; do
		which "${client}" &>/dev/null && 
         DL_CLIENT=${client} && success=1 && break
	done
   [ $success -eq 0 ] &&   
      do_exit 1 "failed to find client in ${DL_CLIENTS}"
fi

tries=0
max_tries=3

get_user_pass $tries || do_exit 1 "failed to get user/pass"
do_auth "${check_url}" "${AUTH_USER}" "${AUTH_PASS}" ;


while ! is_authed "${check_url}"; do
   [ $tries -ge $max_tries ] && 
      do_exit 1 "failed to auth after ${tries} attempts"
	let tries=tries+1
	get_user_pass $tries || do_exit 1 "failed to get user/pass"
   do_auth "${check_url}" "${AUTH_USER}" "${AUTH_PASS}" ;
done


str="authed to ${rhost}"
[ -n "$USED_METHOD" -a $tries -ne 0 ] && 
   str="${str} via ${DL_CLIENT} ${USED_METHOD}"
[ $tries -eq 0 ] && echo "already ${str}" || echo "${str}"

do_exit 0
