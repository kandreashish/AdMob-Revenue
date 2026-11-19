package com.solarad.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solarad.app.model.PastPayout
import com.solarad.app.model.WalletSummary
import com.solarad.app.ui.components.SolarHeader
import com.solarad.app.theme.BorderMedium
import com.solarad.app.theme.BorderSubtle
import com.solarad.app.theme.BotanicalGreen
import com.solarad.app.theme.BotanicalGreenLight
import com.solarad.app.theme.EspressoSlate
import com.solarad.app.theme.LinenSand
import com.solarad.app.theme.MutedSlate
import com.solarad.app.theme.OceanAzure
import com.solarad.app.theme.OceanAzureLight
import com.solarad.app.theme.PureWhite
import com.solarad.app.theme.SolarAmber
import com.solarad.app.theme.SolarAmberLight

@Composable
fun WalletScreen(
    wallet: WalletSummary,
    publisherId: String,
    onProfileClick: () -> Unit,
    onManagePayoutMethods: () -> Unit = {},
    onDownloadReceipt: (PastPayout) -> Unit = {}
) {
    var downloadedId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(LinenSand),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // 1. Header
        item {
            SolarHeader(
                title = "Wallet",
                subtitle = "Payouts & AdSense Balance",
                accountName = publisherId.takeLast(8),
                onAccountClick = onProfileClick
            )
        }

        // 2. Unpaid Balance Hero Card with Threshold Progress
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(26.dp),
                        ambientColor = SolarAmber.copy(alpha = 0.12f),
                        spotColor = Color.Black.copy(alpha = 0.05f)
                    )
                    .border(1.dp, BorderSubtle, RoundedCornerShape(26.dp)),
                color = PureWhite,
                shape = RoundedCornerShape(26.dp)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "UNPAID BALANCE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MutedSlate,
                            letterSpacing = 1.sp
                        )

                        Surface(
                            color = BotanicalGreenLight,
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = BotanicalGreen,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Ready to Pay",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BotanicalGreen
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "$${formatAmount(wallet.unpaidBalance)}",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = EspressoSlate
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Progress bar towards $100 payout threshold
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Payout Threshold: $${wallet.thresholdAmount.toInt()}",
                            fontSize = 12.sp,
                            color = MutedSlate,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${(wallet.thresholdPercent * 100).toInt()}% Met",
                            fontSize = 12.sp,
                            color = BotanicalGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { 1f }, // Cap visually at 100% full
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = BotanicalGreen,
                        trackColor = Color(0xFFEBE7DF),
                        strokeCap = StrokeCap.Round
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Next disbursement banner
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = SolarAmberLight.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SolarAmber.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = SolarAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Next Disbursement: ${wallet.nextDisbursementDate}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EspressoSlate
                                    )
                                    Text(
                                        text = "Initiates in ${wallet.daysRemaining} days via Direct Deposit",
                                        fontSize = 11.sp,
                                        color = MutedSlate
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Linked Payment Method Card
        item {
            Spacer(modifier = Modifier.height(18.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(22.dp),
                        ambientColor = SolarAmber.copy(alpha = 0.08f),
                        spotColor = Color.Black.copy(alpha = 0.04f)
                    )
                    .border(1.dp, BorderSubtle, RoundedCornerShape(22.dp)),
                color = PureWhite,
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PRIMARY PAYMENT METHOD",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MutedSlate,
                            letterSpacing = 1.sp
                        )

                        if (wallet.isBankVerified) {
                            Surface(
                                color = OceanAzureLight,
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VerifiedUser,
                                        contentDescription = null,
                                        tint = OceanAzure,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Verified",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OceanAzure
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = LinenSand,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = EspressoSlate,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = wallet.bankAccountTitle,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = EspressoSlate
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${wallet.bankAccountMasked} • Direct Deposit",
                                fontSize = 12.sp,
                                color = MutedSlate
                            )
                        }

                        ElevatedButton(
                            onClick = onManagePayoutMethods,
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = LinenSand,
                                contentColor = EspressoSlate
                            ),
                            shape = RoundedCornerShape(999.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Manage",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 4. Account Health & Policy Shields
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Ad Serving Card
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp)),
                    color = PureWhite,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = BotanicalGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Surface(
                                shape = CircleShape,
                                color = BotanicalGreenLight
                            ) {
                                Text(
                                    text = "100%",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BotanicalGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Ad Serving",
                            fontSize = 11.sp,
                            color = MutedSlate
                        )
                        Text(
                            text = wallet.adServingStatus,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = EspressoSlate
                        )
                    }
                }

                // Policy Center Card
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp)),
                    color = PureWhite,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = BotanicalGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Surface(
                                shape = CircleShape,
                                color = BotanicalGreenLight
                            ) {
                                Text(
                                    text = "Clean",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BotanicalGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Policy Center",
                            fontSize = 11.sp,
                            color = MutedSlate
                        )
                        Text(
                            text = "${wallet.policyIssuesCount} Issues",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = EspressoSlate
                        )
                    }
                }
            }
        }

        // 5. Past Payout Transfers Archive
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Payout History",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = EspressoSlate
                )

                Text(
                    text = "Last 3 months",
                    fontSize = 12.sp,
                    color = MutedSlate
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        items(wallet.pastPayouts) { payout ->
            PayoutTransferCard(
                payout = payout,
                isDownloaded = downloadedId == payout.id,
                onDownload = {
                    downloadedId = payout.id
                    onDownloadReceipt(payout)
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun PayoutTransferCard(
    payout: PastPayout,
    isDownloaded: Boolean,
    onDownload: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp)),
        color = PureWhite,
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = BotanicalGreenLight,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = BotanicalGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = payout.monthYear,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = EspressoSlate
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Deposited ${payout.depositDate} • ${payout.status}",
                        fontSize = 11.sp,
                        color = MutedSlate
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "$${formatAmount(payout.amount)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = EspressoSlate
                )

                Surface(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isDownloaded) BotanicalGreenLight else LinenSand)
                        .clickable { onDownload() },
                    shape = CircleShape
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.Download,
                            contentDescription = "Download Receipt",
                            tint = if (isDownloaded) BotanicalGreen else EspressoSlate,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatAmount(amount: Double): String {
    val rounded = (amount * 100).toLong() / 100.0
    val parts = rounded.toString().split(".")
    val integerPart = parts[0].reversed().chunked(3).joinToString(",").reversed()
    val decimalPart = if (parts.size > 1) parts[1].padEnd(2, '0').take(2) else "00"
    return "$integerPart.$decimalPart"
}
