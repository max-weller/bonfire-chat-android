<?php
include ".htconfig.php";
if (!$DB_USER) die("Please copy htconfig_template.php to .htconfig.php and fill in the settings");

$db = new PDO("mysql:host=$DB_HOST;dbname=$DB_NAME;charset=utf8", $DB_USER, $DB_PASS);

function errorResult($code, $msg) {
  if ($code == 400) header("HTTP/1.1 400 Bad Request"); else header("HTTP/1.1 500 Internal Server Error");
  echo "$msg\n";
  error_log("ERROR: $msg");
  exit;
}

function base64url_decode($base64url) {
  $base64 = strtr($base64url, '-_', '+/');
  $plainText = base64_decode($base64);
  return ($plainText);
}
