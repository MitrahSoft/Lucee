component {
	this.name = "LDEV5869_" & hash( getCurrentTemplatePath() );
	this.sessionManagement = true;
	this.sessionTimeout = createTimeSpan( 0, 0, 10, 0 );
	this.setClientCookies = false; // Disable cookies so URLSessionFormat adds session to URL
}
