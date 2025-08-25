component extends="org.lucee.cfml.test.LuceeTestCase"{
	
	public function run( testResults , testBox ) {
		describe( "Test suite for LDEV-5766", function() {
            it("Decrypt with 8-byte key (key12345)", function( currentSpec ){
                //encrypt('lucee', 'key12345', 'BLOWFISH', 'Base64')
				var encryptedString = "LFAfnDbOWhg="; //encrypted string
                var key = 'key12345';
                var decryptedString = decrypt(encryptedString, key, "BLOWFISH", "base64");
                expect(decryptedString).toBe("lucee");
			});
            it("Decrypt with 12-byte key (key123456789)", function( currentSpec ){
                //encrypt('lucee', 'key123456789', 'BLOWFISH', 'Base64')
				var encryptedString = "f3Nhq/nfCc8="; //encrypted string
                var key = 'key123456789';
                var decryptedString = decrypt(encryptedString, key, "BLOWFISH", "base64");
                expect(decryptedString).toBe("lucee");
			});
            it("Decrypt with 16-byte key (key1234567891234)", function( currentSpec ){
                //encrypt('lucee', 'key1234567891234', 'BLOWFISH', 'Base64')
				var encryptedString = "Jz5NpVaCrFM="; //encrypted string
                var key = 'key1234567891234';
                var decryptedString = decrypt(encryptedString, key, "BLOWFISH", "base64");
                expect(decryptedString).toBe("lucee");
			});
		});
	}
    
}