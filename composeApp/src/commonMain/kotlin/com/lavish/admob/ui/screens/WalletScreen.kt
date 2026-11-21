package com.lavish.admob.ui.screens

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
import androidx.compose.foundation.lazy.itemsIndexed
import com.lavish.admob.ui.components.uniqueKeys
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lavish.admob.model.PastPayout
import com.lavish.admob.model.WalletSummary
import com.lavish.admob.ui.components.SolarHeader
import com.lavish.admob.ui.utils.formatAmount
import com.lavish.admob.theme.BorderSubtle
import com.lavish.admob.theme.BotanicalGreen
import com.lavish.admob.theme.BotanicalGreenLight
import com.lavish.admob.theme.EspressoSlate
import com.lavish.admob.theme.LinenSand
import com.lavish.admob.theme.MutedSlate
import com.lavish.admob.theme.OceanAzure
import com.lavish.admob.theme.OceanAzureLight
import com.lavish.admob.theme.PureWhite
import com.lavish.admob.theme.SolarAmber
import com.lavish.admob.theme.SolarAmberLight
import com.lavish.admob.theme.SolarTheme

@Composable
fun WalletScreen(
    wallet: WalletSummary,
    publisherId: String,
    accountName: String = "My AdMob",
    avatarUrl: String? = null,
    isOffline: Boolean = false,
    onProfileClick: () -> Unit,
    onManagePayoutMethods: () -> Unit = {},
    onDownloadReceipt: (PastPayout) -> Unit = {}
) {
    var downloadedId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SolarTheme.colors.background),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // 1. Header
        item {
            SolarHeader(
                title = "Wallet",
                subtitle = if (isOffline) "Payouts & AdSense Balance (Cached)" else "Payouts & AdSense Balance",
                accountName = accountName,
                avatarUrl = avatarUrl,
                isOffline = isOffline,
                onAccountClick = onProfileClick
            )
        }

        // 2. Unpaid Balance Hero Card with Threshold Progress
        item {
            Spacer(modifier = Modifier.height(2.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .shadow(
                        elevation = 3.dp,
                        shape = RoundedCornerShape(22.dp),
                        ambientColor = Color.Black.copy(alpha = 0.06f),
                        spotColor = Color.Black.copy(alpha = 0.05f)
                    )
                    .border(1.dp, SolarTheme.colors.borderSubtle, RoundedCornerShape(22.dp)),
                color = SolarTheme.colors.cardBackground,
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
                            text = "UNPAID BALANCE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SolarTheme.colors.textSecondary,
                            letterSpacing = 1.sp
                        )

                        val isThresholdMet = wallet.unpaidBalance >= wallet.thresholdAmount
                        Surface(
                            color = if (isThresholdMet) SolarTheme.colors.successContainer else SolarTheme.colors.surfaceVariant,
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isThresholdMet) Icons.Default.CheckCircle else Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = if (isThresholdMet) BotanicalGreen else SolarTheme.colors.textSecondary,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (isThresholdMet) "Threshold Reached" else "Pacing to $100",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isThresholdMet) BotanicalGreen else SolarTheme.colors.textSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "$${formatAmount(wallet.unpaidBalance)}",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SolarTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Progress bar towards $100 payout threshold
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Payout Threshold: $${wallet.thresholdAmount.toInt()}",
                            fontSize = 11.sp,
                            color = SolarTheme.colors.textSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${(wallet.thresholdPercent * 100).toInt()}% Met",
                            fontSize = 11.sp,
                            color = if (wallet.thresholdPercent >= 1f) BotanicalGreen else SolarTheme.colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { wallet.thresholdPercent.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = BotanicalGreen,
                        trackColor = SolarTheme.colors.surfaceVariant,
                        strokeCap = StrokeCap.Round
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Next disbursement banner
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = SolarTheme.colors.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SolarAmber.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = SolarAmber,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Monthly Cycle: ${wallet.nextDisbursementDate}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SolarTheme.colors.textPrimary
                                )
                                Text(
                                    text = "AdSense transfers automatically once minimum $100 threshold is met",
                                    fontSize = 10.sp,
                                    color = SolarTheme.colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Linked Payment Method Card
        item {
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(20.dp),
                        ambientColor = Color.Black.copy(alpha = 0.06f),
                        spotColor = Color.Black.copy(alpha = 0.04f)
                    )
                    .border(1.dp, SolarTheme.colors.borderSubtle, RoundedCornerShape(20.dp)),
                color = SolarTheme.colors.cardBackground,
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PRIMARY PAYMENT METHOD",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SolarTheme.colors.textSecondary,
                            letterSpacing = 1.sp
                        )

                        if (wallet.isBankVerified) {
                            Surface(
                                color = SolarTheme.colors.secondaryContainer,
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VerifiedUser,
                                        contentDescription = null,
                                        tint = OceanAzure,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Active",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OceanAzure
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = SolarTheme.colors.surfaceVariant,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = SolarTheme.colors.textPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = wallet.bankAccountTitle,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SolarTheme.colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = "${wallet.bankAccountMasked} • AdSense Direct Wire",
                                fontSize = 11.sp,
                                color = SolarTheme.colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        ElevatedButton(
                            onClick = onManagePayoutMethods,
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = SolarTheme.colors.surfaceVariant,
                                contentColor = SolarTheme.colors.textPrimary
                            ),
                            shape = RoundedCornerShape(999.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Manage",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 4. Account Health & Policy Shields
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Ad Serving Card
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, SolarTheme.colors.borderSubtle, RoundedCornerShape(16.dp)),
                    color = SolarTheme.colors.cardBackground,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = BotanicalGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Surface(
                                shape = CircleShape,
                                color = SolarTheme.colors.successContainer
                            ) {
                                Text(
                                    text = "100%",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BotanicalGreen,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Ad Serving",
                            fontSize = 10.sp,
                            color = SolarTheme.colors.textSecondary
                        )
                        Text(
                            text = wallet.adServingStatus,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SolarTheme.colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Policy Center Card
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, SolarTheme.colors.borderSubtle, RoundedCornerShape(16.dp)),
                    color = SolarTheme.colors.cardBackground,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = BotanicalGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Surface(
                                shape = CircleShape,
                                color = SolarTheme.colors.successContainer
                            ) {
                                Text(
                                    text = "Clean",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BotanicalGreen,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Policy Center",
                            fontSize = 10.sp,
                            color = SolarTheme.colors.textSecondary
                        )
                        Text(
                            text = "${wallet.policyIssuesCount} Issues",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SolarTheme.colors.textPrimary
                        )
                    }
                }
            }
        }

        // 5. Past Payout Transfers Archive
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Payout History",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = SolarTheme.colors.textPrimary
                )

                Text(
                    text = "AdSense Disbursements",
                    fontSize = 11.sp,
                    color = SolarTheme.colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        if (wallet.pastPayouts.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    color = SolarTheme.colors.cardBackground,
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SolarTheme.colors.borderSubtle)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No payout records available yet",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SolarTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Google AdSense automatically issues disbursements between the 21st and 26th of the month after your earnings reach $100.",
                            fontSize = 11.sp,
                            color = SolarTheme.colors.textSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            val payoutKeys = uniqueKeys(wallet.pastPayouts) { "payout_" + it.id }
            itemsIndexed(wallet.pastPayouts, key = { i, _ -> payoutKeys[i] }) { _, payout ->
                PayoutTransferCard(
                    payout = payout,
                    isDownloaded = downloadedId == payout.id,
                    onDownload = {
                        downloadedId = payout.id
                        onDownloadReceipt(payout)
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
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
            .padding(horizontal = 16.dp)
            .border(1.dp, SolarTheme.colors.borderSubtle, RoundedCornerShape(16.dp)),
        color = SolarTheme.colors.cardBackground,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Surface(
                    shape = CircleShape,
                    color = SolarTheme.colors.successContainer,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = BotanicalGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = payout.monthYear,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SolarTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = "Deposited ${payout.depositDate} • ${payout.status}",
                        fontSize = 10.sp,
                        color = SolarTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "$${formatAmount(payout.amount)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SolarTheme.colors.textPrimary
                )

                Surface(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isDownloaded) SolarTheme.colors.successContainer else SolarTheme.colors.surfaceVariant)
                        .clickable { onDownload() },
                    shape = CircleShape
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.Download,
                            contentDescription = "Download Receipt",
                            tint = if (isDownloaded) BotanicalGreen else SolarTheme.colors.textPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
