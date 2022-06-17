package de.solidblocks.rds.base

import org.bouncycastle.asn1.DERSequence
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.*
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.util.OpenSSHPrivateKeyUtil
import org.bouncycastle.crypto.util.OpenSSHPublicKeyUtil
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder
import org.bouncycastle.util.io.pem.PemObject
import org.bouncycastle.util.io.pem.PemReader
import java.io.StringReader
import java.io.StringWriter
import java.math.BigInteger
import java.security.*
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*

data class PrivateAndPublicKey(val privateKey: String, val publicKey: String)

class Utils {
    companion object {

        private const val BC_PROVIDER = "BC"
        private const val KEY_ALGORITHM = "RSA"
        private const val SIGNATURE_ALGORITHM = "SHA256withRSA"

        init {
            Security.addProvider(BouncyCastleProvider())
        }

        fun generateSshKey(name: String): PrivateAndPublicKey {

            val generator = Ed25519KeyPairGenerator()
            generator.init(Ed25519KeyGenerationParameters(SecureRandom()))

            val keyPair = generator.generateKeyPair()

            val publicKey = "ssh-ed25519 " + Base64.getEncoder()
                .encodeToString(OpenSSHPublicKeyUtil.encodePublicKey(keyPair.public))

            val content = OpenSSHPrivateKeyUtil.encodePrivateKey(keyPair.private)
            val privateKey = PemObject("OPENSSH PRIVATE KEY", content)

            return PrivateAndPublicKey(privateKey.toPemString(), publicKey)
        }

        fun generateCAKeyPAir(): PrivateAndPublicKey {

            Security.addProvider(BouncyCastleProvider())

            // Initialize a new KeyPair generator
            val keyPairGenerator: KeyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM, BC_PROVIDER)
            keyPairGenerator.initialize(2048)

            // Setup start date to yesterday and end date for 1 year validity
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DATE, -1)
            val startDate = calendar.time

            calendar.add(Calendar.YEAR, 1)
            val endDate = calendar.time

            // First step is to create a root certificate
            // First Generate a KeyPair,
            // then a random serial number
            // then generate a certificate using the KeyPair
            val rootKeyPair = keyPairGenerator.generateKeyPair()
            val rootSerialNum = BigInteger(java.lang.Long.toString(SecureRandom().nextLong()))

            // Issued By and Issued To same for root certificate
            val rootCertIssuer = X500Name("CN=root-cert")
            val rootCertSubject: X500Name = rootCertIssuer
            val rootCertContentSigner =
                JcaContentSignerBuilder(SIGNATURE_ALGORITHM).setProvider(BC_PROVIDER).build(rootKeyPair.private)
            val rootCertBuilder: X509v3CertificateBuilder = JcaX509v3CertificateBuilder(
                rootCertIssuer, rootSerialNum, startDate, endDate, rootCertSubject, rootKeyPair.public
            )

            // Add Extensions
            // A BasicConstraint to mark root certificate as CA certificate
            val rootCertExtUtils = JcaX509ExtensionUtils()
            rootCertBuilder.addExtension(Extension.basicConstraints, true, BasicConstraints(true))
            rootCertBuilder.addExtension(
                Extension.subjectKeyIdentifier, false, rootCertExtUtils.createSubjectKeyIdentifier(rootKeyPair.public)
            )

            /*
            val usageEx = ExtendedKeyUsage(
                arrayOf(
                    KeyPurposeId.id_kp_serverAuth,
                    KeyPurposeId.id_kp_clientAuth
                )
            )
            rootCertBuilder.addExtension(
                Extension.extendedKeyUsage,
                false,
                usageEx.encoded
            )

            val usage = KeyUsage(KeyUsage.keyCertSign or KeyUsage.digitalSignature)
            rootCertBuilder.addExtension(Extension.keyUsage, false, usage.encoded)
            */

            // Create a cert holder and export to X509Certificate
            val rootCertHolder = rootCertBuilder.build(rootCertContentSigner)
            val rootCert =
                JcaX509CertificateConverter().setProvider(BC_PROVIDER).getCertificate(rootCertHolder)

            // writeCertToFileBase64Encoded(rootCert, "root-cert.cer")
            // exportKeyPairToKeystoreFile(rootKeyPair, rootCert, "root-cert", "root-cert.pfx", "PKCS12", "pass")

            val privateKey = PemObject("PRIVATE KEY", rootKeyPair.private.encoded)

            return PrivateAndPublicKey(privateKey.toPemString(), rootCert.toPemString())
        }

        fun createCertificate(rootPrivateKeyRaw: String, rootPublicKeyRaw: String): PrivateAndPublicKey {

            Security.addProvider(BouncyCastleProvider())

            val keyPairGenerator: KeyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM, BC_PROVIDER)
            keyPairGenerator.initialize(2048)

            val rootPrivateKey = rootPrivateKeyRaw.parsePrivateKey()
            val rootPublicKey = rootPublicKeyRaw.parsePublicKey()

            val issuedCertSubject = X500Name("CN=localhost")
            val issuedCertSerialNum = BigInteger(SecureRandom().nextLong().toString())
            val issuedCertKeyPair: KeyPair = keyPairGenerator.generateKeyPair()

            val p10Builder = JcaPKCS10CertificationRequestBuilder(issuedCertSubject, issuedCertKeyPair.public)
            val csrBuilder = JcaContentSignerBuilder(SIGNATURE_ALGORITHM).setProvider(BC_PROVIDER)

            // Sign the new KeyPair with the root cert Private Key
            val csrContentSigner = csrBuilder.build(rootPrivateKey)
            val csr = p10Builder.build(csrContentSigner)

            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DATE, -1)
            val startDate = calendar.time

            calendar.add(Calendar.YEAR, 1)
            val endDate = calendar.time

            // Use the Signed KeyPair and CSR to generate an issued Certificate
            // Here serial number is randomly generated. In general, CAs use
            // a sequence to generate Serial number and avoid collisions
            val issuedCertBuilder = X509v3CertificateBuilder(
                rootPublicKey.issuer, issuedCertSerialNum, startDate, endDate, csr.getSubject(), csr.getSubjectPublicKeyInfo()
            )

            val issuedCertExtUtils = JcaX509ExtensionUtils()

            // Add Extensions
            // Use BasicConstraints to say that this Cert is not a CA
            issuedCertBuilder.addExtension(Extension.basicConstraints, true, BasicConstraints(false))

            // Add Issuer cert identifier as Extension
            issuedCertBuilder.addExtension(
                Extension.authorityKeyIdentifier, false, issuedCertExtUtils.createAuthorityKeyIdentifier(rootPublicKey)
            )
            issuedCertBuilder.addExtension(
                Extension.subjectKeyIdentifier,
                false,
                issuedCertExtUtils.createSubjectKeyIdentifier(csr.getSubjectPublicKeyInfo())
            )

            // Add intended key usage extension if needed
            issuedCertBuilder.addExtension(Extension.keyUsage, false, KeyUsage(KeyUsage.digitalSignature))

            // Add DNS name is cert is to used for SSL
            issuedCertBuilder.addExtension(
                Extension.subjectAlternativeName, false,
                DERSequence(
                    arrayOf(
                        GeneralName(GeneralName.dNSName, "localhost"),
                        GeneralName(GeneralName.iPAddress, "127.0.0.1")
                    )
                )
            )

            val issuedCertHolder = issuedCertBuilder.build(csrContentSigner)
            val issuedCert = JcaX509CertificateConverter().setProvider(BC_PROVIDER).getCertificate(issuedCertHolder)

            // Verify the issued cert signature against the root (issuer) cert
            // issuedCert.verify(rootPublicKey, BC_PROVIDER)

            val privateKey = PemObject("PRIVATE KEY", issuedCertKeyPair.private.encoded)

            return PrivateAndPublicKey(privateKey.toPemString(), issuedCert.toPemString())
        }

        private fun String.parsePrivateKey(): PrivateKey {
            val factory = KeyFactory.getInstance("RSA")

            StringReader(this).use { keyReader ->
                PemReader(keyReader).use { pemReader ->
                    val pemObject = pemReader.readPemObject()
                    val content = pemObject.content
                    val privKeySpec = PKCS8EncodedKeySpec(content)
                    return factory.generatePrivate(privKeySpec)
                }
            }
        }

        private fun String.parsePublicKey(): X509CertificateHolder {
            StringReader(this).use { keyReader ->
                PEMParser(keyReader).use { pemParser ->
                    return pemParser.readObject() as X509CertificateHolder
                }
            }
        }

        private fun PemObject.toPemString() = StringWriter().use { stringWriter ->
            JcaPEMWriter(stringWriter).use { pemWriter ->
                pemWriter.writeObject(this)
            }

            stringWriter.toString()
        }

        private fun X509Certificate.toPemString() = StringWriter().use { stringWriter ->
            JcaPEMWriter(stringWriter).use { pemWriter ->
                pemWriter.writeObject(this)
            }

            stringWriter.toString()
        }
    }
}
